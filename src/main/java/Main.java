import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

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

  private static DNSMessage parseRequest(byte[] arr) {

      int transactionId = arr[0] & 0xFF;
      transactionId = (transactionId << 8) + (arr[1] & 0xFF);

      byte headers1 = arr[2];
      byte opCode = (byte) ((headers1 & 0b01111000) >> 3);
      boolean recursionDesired = ((headers1 & 0b00000001) == 1);

      String domain = parseQuestion(arr);

      return new DNSMessage.Builder()
              .transactionId((short) transactionId)
              .queryIndicator(true)
              .opCode(opCode)
              .recursionDesired(recursionDesired)
              .questionCount((short) 1)
              .question(domain)
              .answer(domain)
              .build();
  }

  private static String parseQuestion(byte[] arr) {
      StringBuilder domain = new StringBuilder();

      int currentIndex = 13;
      byte wordLength = arr[12];
      while (true) {
          byte[] wordBytes = new byte[wordLength];
          System.arraycopy(arr, currentIndex, wordBytes, 0, wordLength);
          domain.append(new String(wordBytes, StandardCharsets.US_ASCII));
          currentIndex += wordLength;
          if (arr[currentIndex] != 0) {
              wordLength = arr[currentIndex];
              currentIndex++;
              domain.append(".");
          } else {
              break;
          }
      }

      return domain.toString();
  }
}
