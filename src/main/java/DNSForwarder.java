import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DNSForwarder {

    private final InetAddress forwardingAddress;
    private final int forwardingPort;

    public DNSForwarder(InetAddress forwardingAddress, int forwardingPort) {

        this.forwardingAddress = forwardingAddress;
        this.forwardingPort = forwardingPort;
    }

    public DNSMessage forwardMessage(DNSMessage message) {

        List<Answer> answers = new ArrayList<>();

        for (Question question : message.getQuestions()) {
            DNSMessage toForward = new DNSMessage.Builder()
                    .transactionId(message.getTransactionId())
                    .queryIndicator(false)
                    .opCode(message.getOpCode())
                    .recursionDesired(message.isRecursionDesired())
                    .questionCount(1)
                    .answerRecordCount(0)
                    .questions(List.of(question))
                    .answers(List.of())
                    .build();

            DNSMessage answer = forwardSingleMessage(toForward);
            // assumes zero or a single answer since we send a single question
            if (!(answer.getAnswers() == null) & !answer.getAnswers().isEmpty()) {
                answers.add(answer.getAnswers().getFirst());
            }
        }

        return new DNSMessage.Builder()
                .transactionId(message.getTransactionId())
                .queryIndicator(true)
                .opCode(message.getOpCode())
                .recursionDesired(message.isRecursionDesired())
                .questionCount(message.getQuestionCount())
                .answerRecordCount(answers.size())
                .questions(message.getQuestions())
                .answers(answers)
                .build();
    }

    private DNSMessage forwardSingleMessage(DNSMessage message) {

        try(DatagramSocket forwardSocket = new DatagramSocket()) {
            System.out.println("forwarding request for domain: " + message.getQuestions().getFirst());

            final byte[] buf = DNSUtils.dnsMessageToByteArray(message);

            DatagramPacket forwardRequestPacket = new DatagramPacket(buf, buf.length, forwardingAddress, forwardingPort);
            forwardSocket.send(forwardRequestPacket);

            final byte[] responseBuf = new byte[512];
            DatagramPacket forwardResponsePacket = new DatagramPacket(responseBuf, responseBuf.length);
            forwardSocket.receive(forwardResponsePacket);

            System.out.println("buffer for forwarding server response: " + Arrays.toString(responseBuf));
            DNSMessage answer = DNSUtils.parsePacket(responseBuf);
            List<Answer> replies = answer.getAnswers();
            if (replies != null && !replies.isEmpty()) {
                Answer reply = replies.getFirst();
                System.out.println("resource from forwarding DNS server: " + reply.resource());
                System.out.println("ttl from forwarding DNS server: " + Arrays.toString(reply.ttl()));
                System.out.println("ip from forwarding DNS server: " + Arrays.toString(reply.ip()));
            } else {
                System.out.println("No answers from server");
            }


            return answer;
        } catch (IOException e) {
            System.out.println("exception while forwarding: " + e.getMessage());
        }

        return null;
    }
}
