import com.nohkumado.ipx800control.*;
import java.util.*;

public class Main
{
	public static void main(String[] args)
	{
		System.out.println("Ipx shell");
		Ipx800Control ipx = new Ipx800Control();
		ipx.setHost("domus.bboett.lan");
		boolean[] relaistate = ipx.getIn();
		System.out.println("Relaistate: " + Arrays.toString(relaistate));
		System.out.println("Counters: " + Arrays.toString(ipx.getCount()));
		System.out.println("Outputs: " + Arrays.toString(ipx.getOut()));
		System.out.println("setting relai 3 to on ");
		ipx.set(3, true);
		System.out.println("Outputs: " + Arrays.toString(ipx.getOut()));
		try
		{
			Thread.sleep(3000);
		}
		catch (InterruptedException e)
		{System.out.println("couldn't wait for 5 s.." + e);}
		System.out.println("setting relai 3 to off ");
		ipx.set(3, false);
		System.out.println("done");
		/*System.out.println("entering questioning");

		 Scanner input = new Scanner(System.in);

		 System.out.print("Enter a number: ");
		 double number1 = input.nextDouble();

		 System.out.print("Enter second number: ");
		 double number2 = input.nextDouble();

		 double product = number1 * number2;
		 System.out.printf("The product of both numbers is: %f", product);
		 */
	}
}
