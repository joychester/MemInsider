package btracevisualmonitor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class BtraceVisualMonitorBak extends Application {

    static final String SPLITER = ",";
    static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    static Date current_date;
    static long modify_counter = 0;
    static String host;
    static String jmxport;
    static String jmxuri;
    static String filepath;
    
    final XYChart.Series<String,Number> series = new XYChart.Series<String,Number>();
    final XYChart.Series<String,Number> series2 = new XYChart.Series<String,Number>();
    final XYChart.Series<String,Number> series3 = new XYChart.Series<String,Number>();
    
    static int count = 0;
    static int default_width = 1000;
    static int default_height = 800;
    static final Image img_icon = new Image(BtraceVisualMonitorBak.class.getResourceAsStream("recycle_bin.png"),50,50,false,false);
    static final Image img_icon2 = new Image(BtraceVisualMonitorBak.class.getResourceAsStream("recycle_logo.png"),50,50,false,false);
    
    final TreeItem<String> gc_timing_sum = new TreeItem<String>("0");
    final TreeItem<String> gc_count_sum = new TreeItem<String>("0");
    final TreeItem<String> gc_avg_sum = new TreeItem<String>("0");
    final TreeItem<String> gc_throughput_sum = new TreeItem<String>("100%");
    
    final TreeItem<String> gctype_timing_1 = new TreeItem<String>("0");
    final TreeItem<String> gctype_count_1 = new TreeItem<String>("0");
    final TreeItem<String> gctype_avg_1 = new TreeItem<String>("0");
    
    final TreeItem<String> gctype_timing_2 = new TreeItem<String>("0");
    final TreeItem<String> gctype_count_2 = new TreeItem<String>("0");
    final TreeItem<String> gctype_avg_2 = new TreeItem<String>("0");
    
    static MBeanServerConnection mbsc;
    static ArrayList<ObjectName> gc_Objname = new ArrayList<ObjectName>();
    
    static int total_gctime = 0;
    static int total_gccount = 0;
    
    protected String getJMXUri(){
        return "service:jmx:rmi:///jndi/rmi://" + host + ":" + jmxport + "/jmxrmi";
    }
    
    protected String getFilePath(){
        return filepath;
    }
    private void init(Stage primaryStage) {
        Group root = new Group();
        primaryStage.setScene(new Scene(root, default_width, default_height));

        //Create Gc detail info Hbox element to the root
        final ImageView imgview1 = new ImageView(img_icon);
        final Label gclabel = new Label("GC Info:", imgview1);
        gclabel.setContentDisplay(ContentDisplay.LEFT);
        
        final TreeItem<String> treeroot = new TreeItem<String>("GC Summary");
        treeroot.getChildren().addAll(Arrays.asList(
                new TreeItem<String>("GC Timing(ms)"),
                new TreeItem<String>("GC Count"),
                new TreeItem<String>("GC Avg(ms)"),
                new TreeItem<String>("GC Throughput")));

        treeroot.getChildren().get(0).getChildren().add(
                gc_timing_sum);
        treeroot.getChildren().get(1).getChildren().add(
                gc_count_sum);
        treeroot.getChildren().get(2).getChildren().add(
                gc_avg_sum);
        treeroot.getChildren().get(3).getChildren().add(
                gc_throughput_sum);
        
        final TreeView treeview = new TreeView();
        treeview.setShowRoot(true);
        treeview.setRoot(treeroot);
        treeview.setMaxSize(130, 160);
        treeroot.setExpanded(true);        

        HBox hbox = new HBox(5);
        hbox.getChildren().addAll(gclabel,treeview);
        hbox.setAlignment(Pos.CENTER);
        hbox.setLayoutX(50);
        hbox.setLayoutY(630);
        root.getChildren().add(hbox);
        
       
        //Create Gc detail info Hbox element to the root
        final ImageView imgview2 = new ImageView(img_icon2);
        final Label gclabel2 = new Label("GC Details:", imgview2);
        gclabel2.setContentDisplay(ContentDisplay.LEFT);
         //Add Gc type name to the root
        List<String> gc_types= getGCTypeName(getJMXUri());
        final TreeItem<String> treeroot2 = new TreeItem<String>("GC Type Detail");
        treeroot2.getChildren().addAll(Arrays.asList(
                new TreeItem<String>(gc_types.get(0)),
                new TreeItem<String>(gc_types.get(1))));
        treeroot2.getChildren().get(0).getChildren().addAll(Arrays.asList(
                new TreeItem<String>("Acc Time(ms)"),
                new TreeItem<String>("Total Count"),
                new TreeItem<String>("Avg Time(ms)")));
        treeroot2.getChildren().get(1).getChildren().addAll(Arrays.asList(
                new TreeItem<String>("Acc Time(ms)"),
                new TreeItem<String>("Total Count"),
                new TreeItem<String>("Avg Time(ms)")));
        
        treeroot2.getChildren().get(0).getChildren().get(0).getChildren().add(
                gctype_timing_1);
        treeroot2.getChildren().get(0).getChildren().get(1).getChildren().add(
                gctype_count_1);
        treeroot2.getChildren().get(0).getChildren().get(2).getChildren().add(
                gctype_avg_1);
        treeroot2.getChildren().get(1).getChildren().get(0).getChildren().add(
                gctype_timing_2);
        treeroot2.getChildren().get(1).getChildren().get(1).getChildren().add(
                gctype_count_2);
        treeroot2.getChildren().get(1).getChildren().get(2).getChildren().add(
                gctype_avg_2);
        
        final TreeView treeview2 = new TreeView();
        treeview2.setShowRoot(true);
        treeview2.setRoot(treeroot2);
        treeview2.setMaxSize(150, 160);
        treeroot2.setExpanded(true);
        
        //root.getChildren().add(treeview2);
        HBox hbox2 = new HBox(5);
        hbox2.getChildren().addAll(gclabel2,treeview2);
        hbox2.setAlignment(Pos.CENTER);
        hbox2.setLayoutX(650);
        hbox2.setLayoutY(630);
        root.getChildren().add(hbox2);
        
        //Add Chart to the Root
        root.getChildren().add(createChart());
    }

    protected List<String> getGCTypeName(String jmxpath){
        List<String> e = new ArrayList<String>();
        
        try {
               
               mbsc = initMBeanServerConn(jmxpath);

               Iterator<ObjectName> itr = mbsc.queryNames(null, null).iterator();
               while( itr.hasNext() ) {
                     ObjectName item = itr.next();
                     
                     if (item.toString().indexOf("GarbageCollector")!= -1){
                         gc_Objname.add(item);
                         e.add(item.toString().split("=")[2]);
                     }
                }
        } catch (IOException ex) {
            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
        }
        return e;
    }
    
    public MBeanServerConnection initMBeanServerConn(String jmxuri){
        MBeanServerConnection mbc = null;
        try {
            JMXServiceURL url =
                    new JMXServiceURL(jmxuri);
            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            
            mbc = jmxc.getMBeanServerConnection();
        } catch (IOException ex) {
            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mbc;
    }
    
    protected LineChart<String, Number> createChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final LineChart<String,Number> lc = new LineChart<String,Number>(xAxis,yAxis);
        // Setup LineChart
        lc.setTitle("Memory Chart");
        lc.setPrefSize(default_width*0.95, default_height*0.8);

        xAxis.setLabel("Time");
        yAxis.setLabel("Mem Usage");

        series.setName("Used Heap");
        series2.setName("Used NonHeap");
        series3.setName("Avg GC Line");
        
        lc.getData().addAll(series,series2,series3);

        return lc;
    }    
    
    
    @Override public void start(final Stage primaryStage) throws Exception {
        
        init(primaryStage);
        primaryStage.show();
                        
        FileMonitor monitor = new FileMonitor (1000);
        final File file = new File(getFilePath());
        monitor.addFileWithSize(file);
        
        monitor.addListener(new FileListener(){
            
            // when file changed, read data from file
            @Override
            public void fileChanged(final File file){
                //switch to JavaFX main thread
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        current_date = new Date();
                        String time_line = dateFormat.format(current_date);
                        try {
                            FileReader fr = new FileReader(file);
                            LineNumberReader lnr =new LineNumberReader(fr);
                            String currentline = ""; 
                            for (int i = 0; i < count; i++) {
                                lnr.readLine(); 
                            }
                            while ((currentline = lnr.readLine())!=null){
                                String[] a = currentline.split(SPLITER);

                                //just ignore the legacy data or the first line of data in the file
                                if((!"".equals(a[0].trim())) & modify_counter>0){
                                    if(a.length>1){
                                        series.getData().add(new XYChart.Data<String,Number>(time_line, Integer.valueOf(a[0].trim())));
                                        series2.getData().add(new XYChart.Data<String,Number>(time_line, Integer.valueOf(a[1].trim())));
                                    }
                                    else{
                                        series.getData().add(new XYChart.Data<String,Number>(time_line, Integer.valueOf(a[0].trim())));
                                    }
                                    
                                }
                                
                                gc_timing_sum.setValue(a[2].trim());
                                gc_count_sum.setValue(a[3].trim());
                                gc_avg_sum.setValue(a[4].trim());
                                gc_throughput_sum.setValue(a[5].trim());
                                
                            }
                            count = lnr.getLineNumber();
                            
                            //get some gc info from Mbeans directly
                            String time_0 = mbsc.getAttribute(gc_Objname.get(0), "CollectionTime").toString();
                            String count_0 = mbsc.getAttribute(gc_Objname.get(0), "CollectionCount").toString();
                            int time_0_int = Integer.valueOf(time_0);
                            int count_0_int = Integer.valueOf(count_0);
                            
                            gctype_timing_1.setValue(time_0);
                            gctype_count_1.setValue(count_0);
                            if(!count_0.equals("0")){
                                gctype_avg_1.setValue(String.valueOf((time_0_int/count_0_int)));
                            }
                            
                            String time_1 = mbsc.getAttribute(gc_Objname.get(1), "CollectionTime").toString();
                            String count_1 = mbsc.getAttribute(gc_Objname.get(1), "CollectionCount").toString();
                            int time_1_int = Integer.valueOf(time_1);
                            int count_1_int = Integer.valueOf(count_1);
                            
                            gctype_timing_2.setValue(time_1);
                            gctype_count_2.setValue(count_1);
                            if(!count_1.equals("0")){
                                gctype_avg_2.setValue(String.valueOf((time_1_int/count_1_int)));
                            }
                            
                            // draw the Gc time line
                            int gc_avg_line = 0;
                            if(count_0_int+count_1_int-total_gccount!=0){
                                gc_avg_line = ((time_0_int+time_1_int-total_gctime)/(count_0_int+count_1_int-total_gccount));
                            }
                            //ignore the very first data reading from mbeans to limit the data redundant issue
                            if(modify_counter > 0){
                                series3.getData().add(new XYChart.Data<String,Number>(time_line, gc_avg_line*100000));
                            }

                            total_gctime = time_0_int+time_1_int;
                            total_gccount = count_0_int+count_1_int;
                            
                            modify_counter++;
                            
                            lnr.close();
                            fr.close();
                        } catch (MBeanException ex) {
                            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (AttributeNotFoundException ex) {
                            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InstanceNotFoundException ex) {
                            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ReflectionException ex) {
                            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(BtraceVisualMonitorBak.class.getName()).log(Level.SEVERE, null, ex);
                        }
                   }
                });
            }
        });
    }
    public static void main(String[] args) {
        // init the global para, at least you need to provide the file path
        if(args.length > 1){
            filepath = args[0]; 
            host = args[1];
            jmxport = args[2];
        }
        else //default port 9426
        {
            filepath = args[0];
            host = "localhost";
            jmxport = "9426";
        }

        launch(args); 
    }
}
