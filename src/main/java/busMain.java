import com.google.maps.errors.ApiException;

import java.io.IOException;
import java.util.Scanner;

public class busMain {

    public static void main(String[] args) {
        System.out.println("Welcome To Madison Bus Tracker");
        runTracker();
    }


    private static void runTracker() {
        Scanner scanner = new Scanner(System.in);
        String src, dest;

        System.out.println(" TO START, PLEASE ENTER YOUR LOCATION: ");
        src = scanner.nextLine();
        System.out.println(" PLEASE ENTER YOUR DESTINATION");
        dest = scanner.nextLine();

        try {
            travel.createInstance(src, dest);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("END");
    }
}