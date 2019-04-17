import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Dummy {

	private static final NumberFormat DECIMAL = new DecimalFormat("#.###");

	@Test
	public void dummy() {
		System.out.println(DECIMAL.format(Float.valueOf("NaN")));
	}
}
