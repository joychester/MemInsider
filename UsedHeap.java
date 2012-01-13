package sample.cpu.mine;

import com.sun.btrace.annotations.*;
import com.sun.btrace.aggregation.*;
import static com.sun.btrace.BTraceUtils.*;
import java.text.DecimalFormat;

@BTrace
public class UsedHeap {
	@TLS
	static int loopcount = 0;
	
	@OnTimer(5000)
	public static void printAll() {

		Appendable mem = Strings.newStringBuilder();
		Strings.append(mem, str(used(heapUsage())));
		Strings.append(mem, ",");
		Strings.append(mem, str(used(nonHeapUsage())));
		Strings.append(mem, ",");
		Strings.append(mem, str(getTotalGcTime()));
		Strings.append(mem, ",");
		Strings.append(mem, str(getTotalCollectionCount()));
		Strings.append(mem, ",");
		Strings.append(mem, str(getTotalGcTime()/getTotalCollectionCount()));
		Strings.append(mem, ",");
		Strings.append(mem, getGCThroughput());
		
		println(str(mem));

	}

}