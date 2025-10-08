import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
  public static void main(String[] args){

     try(DatagramSocket serverSocket = new DatagramSocket(2053)) {
       while(true) {
         final byte[] buf = new byte[512];
         final DatagramPacket packet = new DatagramPacket(buf, buf.length);
         serverSocket.receive(packet);
         System.out.println("Received data");
         short transactionId = parseTransactionId(buf);

         DNSMessage response = new DNSMessage.Builder()
                 .transactionId(transactionId)
                 .queryIndicator(true)
                 .questionCount((short) 1)
                 .question("codecrafters.com")
                 .build();

         final byte[] bufResponse = response.toByteArray();
         final DatagramPacket packetResponse = new DatagramPacket(bufResponse, bufResponse.length, packet.getSocketAddress());
         serverSocket.send(packetResponse);
       }
     } catch (IOException e) {
         System.out.println("IOException: " + e.getMessage());
     }
  }

  private static short parseTransactionId(byte[] arr) {

      int value = arr[0] & 0xFF;
      value = (value << 8) + (arr[1] & 0xFF);
      return (short) value;
  }
}
