package com.example.data.network

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress

object DnsPacketCoder {

    data class DnsResponse(
        val rCode: Int,
        val rCodeName: String,
        val answers: List<DnsRecord>
    )

    data class DnsRecord(
        val name: String,
        val type: Int,
        val typeName: String,
        val ttl: Long,
        val data: String
    )

    fun buildQuery(domain: String, typeString: String): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        // 1. Transaction ID (2 bytes)
        dos.writeShort(1234) // 0x04D2

        // 2. Flags: Recursion Desired (RD = 1) -> 0x0100 (2 bytes)
        dos.writeShort(0x0100)

        // 3. Questions Count: 1 (2 bytes)
        dos.writeShort(1)

        // 4. Answer RRs: 0 (2 bytes)
        dos.writeShort(0)

        // 5. Authority RRs: 0 (2 bytes)
        dos.writeShort(0)

        // 6. Additional RRs: 0 (2 bytes)
        dos.writeShort(0)

        // 7. Question Section: Domain Name (variable)
        // Split by '.' but handle any trailing dots gracefully
        val cleanDomain = if (domain.endsWith(".")) domain.substring(0, domain.length - 1) else domain
        val labels = cleanDomain.split(".")
        for (label in labels) {
            if (label.isNotEmpty()) {
                val bytes = label.toByteArray(Charsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
        }
        dos.writeByte(0) // Zero length byte indicating end of name

        // 8. Question Section: Type (2 bytes)
        val typeInt = getTypeCode(typeString)
        dos.writeShort(typeInt)

        // 9. Question Section: Class: IN = 1 (2 bytes)
        dos.writeShort(1)

        return bos.toByteArray()
    }

    fun parseResponse(bytes: ByteArray): DnsResponse {
        val buffer = DnsBuffer(bytes)
        
        // Header
        val id = buffer.readUnsignedShort()
        val flags = buffer.readUnsignedShort()
        val qdCount = buffer.readUnsignedShort()
        val anCount = buffer.readUnsignedShort()
        val nsCount = buffer.readUnsignedShort()
        val arCount = buffer.readUnsignedShort()

        val rCode = flags and 0x000F
        val rCodeName = getRCodeName(rCode)

        // Skip Questions
        for (i in 0 until qdCount) {
            buffer.readName() // consumes name
            buffer.readUnsignedShort() // type
            buffer.readUnsignedShort() // class
        }

        // Parse Answers
        val answers = mutableListOf<DnsRecord>()
        for (i in 0 until anCount) {
            try {
                val name = buffer.readName()
                val type = buffer.readUnsignedShort()
                val clazz = buffer.readUnsignedShort()
                val ttl = buffer.readInt().toLong() and 0xFFFFFFFFL
                val rdLength = buffer.readUnsignedShort()

                val dataStr = buffer.parseRData(type, rdLength)
                answers.add(DnsRecord(name, type, getTypeName(type), ttl, dataStr))
            } catch (e: Exception) {
                // If parsing a record fails, log it and possibly exit or break
                answers.add(DnsRecord("malformed_record", 0, "UNKNOWN", 0, "Error parsing record: ${e.localizedMessage}"))
                break
            }
        }

        return DnsResponse(rCode, rCodeName, answers)
    }

    fun getTypeCode(typeStr: String): Int {
        return when (typeStr.uppercase()) {
            "A" -> 1
            "NS" -> 2
            "CNAME" -> 5
            "MX" -> 15
            "TXT" -> 16
            "AAAA" -> 28
            else -> 1
        }
    }

    fun getTypeName(type: Int): String {
        return when (type) {
            1 -> "A"
            2 -> "NS"
            5 -> "CNAME"
            15 -> "MX"
            16 -> "TXT"
            28 -> "AAAA"
            else -> "TYPE_$type"
        }
    }

    fun getRCodeName(rCode: Int): String {
        return when (rCode) {
            0 -> "NOERROR"
            1 -> "FORMERR"
            2 -> "SERVFAIL"
            3 -> "NXDOMAIN"
            4 -> "NOTIMP"
            5 -> "REFUSED"
            else -> "RCODE_$rCode"
        }
    }

    // Helper class to parse bytes sequentially
    class DnsBuffer(val bytes: ByteArray) {
        var ptr = 0

        fun readUnsignedByte(): Int {
            if (ptr >= bytes.size) return 0
            return (bytes[ptr++].toInt() and 0xFF)
        }

        fun readUnsignedShort(): Int {
            val b1 = readUnsignedByte()
            val b2 = readUnsignedByte()
            return (b1 shl 8) or b2
        }

        fun readInt(): Int {
            val b1 = readUnsignedByte()
            val b2 = readUnsignedByte()
            val b3 = readUnsignedByte()
            val b4 = readUnsignedByte()
            return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }

        fun readBytes(len: Int): ByteArray {
            val res = ByteArray(len)
            val count = Math.min(len, bytes.size - ptr)
            if (count > 0) {
                System.arraycopy(bytes, ptr, res, 0, count)
                ptr += count
            }
            return res
        }

        fun readName(): String {
            val sb = StringBuilder()
            readNameAt(ptr, sb)
            advancePastName()
            val res = sb.toString()
            return if (res.endsWith(".")) res.substring(0, res.length - 1) else res
        }

        private fun readNameAt(offset: Int, sb: StringBuilder) {
            var currentOffset = offset
            var visited = 0
            while (visited < 64 && currentOffset < bytes.size) {
                val len = bytes[currentOffset].toInt() and 0xFF
                if (len == 0) {
                    break
                }
                if ((len and 0xC0) == 0xC0) {
                    if (currentOffset + 1 >= bytes.size) break
                    val nextByte = bytes[currentOffset + 1].toInt() and 0xFF
                    val pointerOffset = ((len and 0x3F) shl 8) or nextByte
                    readNameAt(pointerOffset, sb)
                    return
                } else {
                    currentOffset++
                    if (currentOffset + len > bytes.size) break
                    val label = String(bytes, currentOffset, len, Charsets.UTF_8)
                    sb.append(label).append(".")
                    currentOffset += len
                }
                visited++
            }
        }

        private fun advancePastName() {
            while (ptr < bytes.size) {
                val len = bytes[ptr].toInt() and 0xFF
                if (len == 0) {
                    ptr++ // consume 0
                    return
                }
                if ((len and 0xC0) == 0xC0) {
                    ptr += 2 // consume pointer representation
                    return
                }
                ptr += 1 + len // consume length byte + label characters
            }
        }

        fun parseRData(type: Int, rdLength: Int): String {
            val startOfRData = ptr
            val res = when (type) {
                1 -> { // A record (IPv4 Address)
                    if (rdLength == 4) {
                        "${readUnsignedByte()}.${readUnsignedByte()}.${readUnsignedByte()}.${readUnsignedByte()}"
                    } else {
                        "Malformed A"
                    }
                }
                28 -> { // AAAA record (IPv6 Address)
                    if (rdLength == 16) {
                        val ipBytes = readBytes(16)
                        try {
                            InetAddress.getByAddress(ipBytes).hostAddress ?: "Invalid IPv6"
                        } catch (e: Exception) {
                            "Error parsing IPv6"
                        }
                    } else {
                        "Malformed AAAA"
                    }
                }
                2, 5 -> { // NS or CNAME record
                    val tmp = DnsBuffer(bytes)
                    tmp.ptr = startOfRData
                    val name = tmp.readName()
                    ptr += rdLength
                    name
                }
                15 -> { // MX record
                    val preference = readUnsignedShort()
                    val tmp = DnsBuffer(bytes)
                    tmp.ptr = ptr
                    val exchange = tmp.readName()
                    ptr = startOfRData + rdLength
                    "[$preference] $exchange"
                }
                16 -> { // TXT record
                    val textBuilder = StringBuilder()
                    var bytesRead = 0
                    while (bytesRead < rdLength && ptr < bytes.size) {
                        val textLen = readUnsignedByte()
                        bytesRead++
                        if (textLen <= rdLength - bytesRead) {
                            textBuilder.append(String(readBytes(textLen), Charsets.UTF_8))
                            bytesRead += textLen
                        } else {
                            break
                        }
                    }
                    textBuilder.toString()
                }
                else -> {
                    // Raw hex characters representation
                    val hexBytes = readBytes(rdLength)
                    hexBytes.joinToString("") { String.format("%02X", it) }
                }
            }
            ptr = startOfRData + rdLength // ensure the pointer is advanced reliably
            return res
        }
    }
}
