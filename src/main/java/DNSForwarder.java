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

            Answer answer = forwardSingleMessage(toForward);
            // assumes a single answer since we send a single question
            answers.add(answer);
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

    private Answer forwardSingleMessage(DNSMessage message) {

        Answer answer = Answer.defaultAnswer(message.getQuestions().getFirst());

        try(DatagramSocket forwardSocket = new DatagramSocket()) {
            System.out.println("forwarding request for domain: " + message.getQuestions().getFirst());

            final byte[] buf = DNSUtils.dnsMessageToByteArray(message);

            DatagramPacket forwardRequestPacket = new DatagramPacket(buf, buf.length, forwardingAddress, forwardingPort);
            forwardSocket.send(forwardRequestPacket);

            final byte[] responseBuf = new byte[512];
            DatagramPacket forwardResponsePacket = new DatagramPacket(responseBuf, responseBuf.length);
            forwardSocket.receive(forwardResponsePacket);

            System.out.println("received response from forwarding server");
            answer = DNSUtils.parseAnswer(responseBuf);
            System.out.println("answer from forwarding DNS server: " + answer.resource());
        } catch (IOException e) {
            System.out.println("exception while forwarding: " + e.getMessage());
        }

        return answer;
    }
}
