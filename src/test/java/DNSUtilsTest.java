import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DNSUtilsTest {

    @Test
    void parsePacket_shouldConstructCorrectMessage() {

        byte[] requestBytes = HexFormat.ofDelimiter(" ")
                .parseHex("04 d2 22 10 00 01 00 00 00 00 00 00 "
                        + "0c 63 6f 64 65 63 72 61 66 74 65 72 73 02 69 6f 00");

        DNSMessage request = DNSUtils.parsePacket(requestBytes);
        assertThat(request.getTransactionId()).isEqualTo((short) 1234);
        assertThat(request.getOpCode()).isEqualTo((byte) 4);
        assertThat(request.getQuestionCount()).isEqualTo(1);
        assertThat(request.getQuestions().getFirst().question()).isEqualTo("codecrafters.io");
    }

    @Test
    void parsePacket_withMultipleQuestions_shouldConstructCorrectMessage() {

        byte[] requestBytes = HexFormat.ofDelimiter(" ")
                .parseHex("04 d2 22 10 00 02 00 00 00 00 00 00 "
                        + "0c 63 6f 64 65 63 72 61 66 74 65 72 73 02 69 6f 00 00 01 00 01 "
                        + "05 63 68 65 63 6b 0c 63 6f 64 65 63 72 61 66 74 65 72 73 02 69 6f 00 00 01 00 01");

        DNSMessage request = DNSUtils.parsePacket(requestBytes);
        assertThat(request.getTransactionId()).isEqualTo((short) 1234);
        assertThat(request.getOpCode()).isEqualTo((byte) 4);
        assertThat(request.getQuestionCount()).isEqualTo(2);
        assertThat(request.getQuestions().get(0).question()).isEqualTo("codecrafters.io");
        assertThat(request.getQuestions().get(1).question()).isEqualTo("check.codecrafters.io");
    }

//    @Test
//    void parsePacket_withCompressedQuestion_shouldConstructCorrectMessage() {
//
//        byte[] requestBytes = HexFormat.ofDelimiter(" ")
//                .parseHex("04 d2 22 10 00 02 00 00 00 00 00 00 "
//                        + "0c 63 6f 64 65 63 72 61 66 74 65 72 73 02 69 6f 00 00 01 00 01 "
//                        + "05 63 68 65 63 6b cc 00 00 01 00 01");
//
//        DNSMessage request = DNSUtils.parsePacket(requestBytes);
//        assertThat(request.getTransactionId()).isEqualTo((short) 1234);
//        assertThat(request.getOpCode()).isEqualTo((byte) 4);
//        assertThat(request.getQuestionCount()).isEqualTo(2);
//        assertThat(request.getQuestions().get(0).question()).isEqualTo("codecrafters.io");
//        assertThat(request.getQuestions().get(1).question()).isEqualTo("check.codecrafters.io");
//    }
//
//    @Test
//    void dnsResponse_shouldContainUncompressedQuestions() {
//
//        byte[] requestBytes = HexFormat.ofDelimiter(" ")
//                .parseHex("04 d2 22 10 00 02 00 00 00 00 00 00 "
//                        + "0c 63 6f 64 65 63 72 61 66 74 65 72 73 02 69 6f 00 00 01 00 01 "
//                        + "05 63 68 65 63 6b cc 00 00 01 00 01");
//
//        DNSMessage request = DNSUtils.parsePacket(requestBytes);
//        byte[] response = DNSUtils.dnsMessageToByteArray(request);
//
//        assertThat(response[12]).isEqualTo((byte) 12);
//    }
}