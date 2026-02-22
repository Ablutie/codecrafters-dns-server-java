import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
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
        for (String question : message.getQuestions()) {
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

            DNSMessage response = forwardSingleMessage(toForward);
            // assumes a single answer since we send a single question
            answers.add(response.getAnswers().getFirst());
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

        DNSMessage response = null;
        try(DatagramSocket forwardSocket = new DatagramSocket()) {
            System.out.println("forwarding request");

            final byte[] buf = DNSUtils.dnsMessageToByteArray(message);

            DatagramPacket forwardRequestPacket = new DatagramPacket(buf, buf.length, forwardingAddress, forwardingPort);
            forwardSocket.send(forwardRequestPacket);

            DatagramPacket forwardResponsePacket = new DatagramPacket(buf, buf.length);
            forwardSocket.receive(forwardResponsePacket);

            System.out.println("received response from forwarding server");
            response = DNSUtils.parsePacket(buf, true);
            for (Answer answer : response.getAnswers()) {
                System.out.println("answer from forwarding DNS server: " + answer.resource());
            }
        } catch (IOException e) {
            System.out.println("exception while forwarding: " + e.getMessage());
        }

        return response;
    }
}
