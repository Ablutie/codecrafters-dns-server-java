import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {

        int forwardingPort = 53;
        String forwardingAddress = "localhost";

        if (args.length != 0) {
            String argument = args[0];
            if (!(argument == null) && argument.equals("--resolver")) {
                System.out.println("Need to forward to address: " + args[1]);
                String[] splitArgs = args[1].split(":");
                forwardingPort = Integer.parseInt(splitArgs[1]);
                forwardingAddress = splitArgs[0];
            }
        } else {
            System.out.println("No need to forward, will echo request");
        }

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data: " + Arrays.toString(buf));

                DNSForwarder forwarder = new DNSForwarder(InetAddress.getByName(forwardingAddress), forwardingPort);

                DNSMessage request = DNSUtils.parsePacket(buf);
                for (Question question : request.getQuestions()) {
                    System.out.println("Request contains question: " + question.question());
                }

                byte[] bufResponse;

                DNSMessage response;
                if (!shouldForward(args)) {
                    response = DNSUtils.echoMessage(request);
                } else {
                    response = forwarder.forwardMessage(request);
                }
                bufResponse = DNSUtils.dnsMessageToByteArray(response);

                final DatagramPacket packetResponse = new DatagramPacket(bufResponse, bufResponse.length,
                        packet.getSocketAddress());

                serverSocket.send(packetResponse);

            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static boolean shouldForward(String[] args) {

        if (args.length == 0) {
            return false;
        }
        return args.length == 2 && args[0].equals("--resolver");
    }

}
