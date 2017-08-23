package buaa;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.aliyun.odps.data.Record;
import com.aliyun.odps.mapred.ReducerBase;


public class MyReducer extends ReducerBase {
	static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");  
	private Record result = null;
	@Override
	public void setup(TaskContext context) throws IOException {
		result = context.createOutputRecord();
	}

	@Override
	public void reduce(Record key, Iterator<Record> values, TaskContext context)
			throws IOException {
		System.out.println(MyDriver.baseHour);
		String link=key.get(0).toString();
		String[] ktime= new String[153];
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(sdf.parse("2016-06-30"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=0;i<31;i++){
			c.add(Calendar.DATE, 1); //日期加1天  
			ktime[i] = sdf.format(c.getTime());  
		}
		try {
			c.setTime(sdf.parse("2017-03-31"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=31;i<ktime.length;i++){
			c.add(Calendar.DATE, 1); //日期加1天  
			ktime[i] = sdf.format(c.getTime());  
		}
		double min_value=1000;
		ArrayList<String[]> vals=new ArrayList<String[]>();
		while (values.hasNext()) {
			String line=(String)values.next().get(0);
			String[] lines=line.split("#");
			for(int i=2;i<lines.length-1;i++){
				double cuval=Double.valueOf(lines[i]);
				if(cuval>0&&cuval<min_value){
					min_value=cuval;
				}
			}
			vals.add(lines);
		}
		double[] init= new double[90];
		for(int i=0;i<init.length;i++){
			init[i]=min_value;
		}
		HashMap<String,double[]> kv = new HashMap<String,double[]>();
		for (String kt : ktime) {
			kv.put(kt, init);
		}
		for (String[] line : vals) {
			for(int i=2;i<line.length;i++){
				double cuval=Double.valueOf(line[i]);
				int s_cur=i-1;
				int b_cur=i+1;
				double la=0;
				double wa=0;
				if(cuval==0){
					while(s_cur>=2){
						if(Double.valueOf(line[s_cur])>0){
							la=Double.valueOf(line[s_cur]);
							break;
						}
						s_cur = s_cur-1;
					}
					while(b_cur<line.length){
						if(Double.valueOf(line[b_cur])>0){
							wa=Double.valueOf(line[b_cur]);
							break;
						}
						b_cur = b_cur+1;
					}
					if(wa==0 || (la>0 && la<wa)){
						line[i]=String.valueOf(la);
					}else{
						line[i]=String.valueOf(wa);
					}
				}
			}
			String mydate=(String)line[1];
			double[] fillline= new double[90];
			for(int i=2;i<line.length;i++){
				fillline[i-2]=Double.valueOf(line[i]);
			}
			kv.put(mydate, fillline);
		}
		for (String k : kv.keySet()) {
			double[] v = kv.get(k);
			result.set(0,link);
			result.set(1, k);
			for(int i=0;i<v.length;i++){
				result.set(i+2,v[i]);
			} 
			context.write(result);
		}
		
	}

	@Override
	public void cleanup(TaskContext context) throws IOException {
	}

}
