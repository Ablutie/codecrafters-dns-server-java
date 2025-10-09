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

         DNSMessage response = parseRequest(buf);

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

  private static DNSMessage parseRequest(byte[] arr) {

      int transactionId = arr[0] & 0xFF;
      transactionId = (transactionId << 8) + (arr[1] & 0xFF);

      byte headers1 = arr[2];
      byte opCode = (byte) ((headers1 & 0b01111000) >> 3);
      boolean recursionDesired = ((headers1 & 0b00000001) == 1);

      return new DNSMessage.Builder()
              .transactionId((short) transactionId)
              .queryIndicator(true)
              .opCode(opCode)
              .recursionDesired(recursionDesired)
              .questionCount((short) 1)
              .question("codecrafters.io")
              .answer("codecrafters.io")
              .build();
  }
}
