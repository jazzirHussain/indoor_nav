package com.sample_project.mitsmap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import static android.content.Context.SENSOR_SERVICE;

public class NavigationFragment extends Fragment implements SensorEventListener {
    HashMap<String, Integer> fixedMACBLE = new HashMap<String, Integer>();
    HashMap<String, Integer> BLE_FLOOR = new HashMap<String, Integer>();
    Button btn_location,btn_viewmaps;
    // Storage for Sensor readings
    private static float[] mGeomagnetic = null;
    private long pressedTime;
    static String className=null;
    private Handler handler = new Handler();
    static ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
    public static int x;
    public static int y;
    static int GridX,GridY;
    static boolean floor_check=false;
    //weka
    private static final String WEKA_TEST = "WekaTest";
    static ArrayList<String> classVal = new ArrayList<String>();
    DecimalFormat decimalFormatter;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    boolean scanning = false;
    private SensorManager sensorManager;
    private DatabaseManger dbmanager;
    ArrayList<String> listBluetoothDevice;
    Map<String, String> device_rssi_dynamic ;
    Map<String, Integer> device_scan_no;

    //file
    private static final String FILE_NAME = "walk_analysis.txt";
    private static int datacollector=1;
    FileOutputStream fos;
    // Class members for each sensor reading of interest
    private static float accelerometerX;
    private static float accelerometerY;
    private static float accelerometerZ;
    private static float magneticX;
    private static float magneticY;
    private static float magneticZ;
    private static float light;
    private static float rotationX;
    private static float rotationY;
    private static float rotationZ;
    private static float[] rotation;
    private static float[] inclination;
    private static float[] orientation;
    private List<Sensor> sensorList;


    private static int collectionCount=0;
    private Context mContext;
    static String rssi_old;
    TextView textView,textView1,textView4;
    Spinner dropdown;
    Button btn_nav;
    String[] floor = new String[] {
            "Lift", "Electronics Workshop", "Digital Electronics Lab", "Classroom 1","Analog Communication Lab", "ECE Staffroom", "Communication Lab", "classroom 2", "Classroom 3", "Tutorial room"
    };

    int x_flag = 1,scan_val = 0;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
        //   dbmanager=new DatabaseManger(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {


        decimalFormatter = new DecimalFormat("#");
        decimalFormatter.setMaximumFractionDigits(8);
        btManager = (BluetoothManager) getActivity().getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        dbmanager=new DatabaseManger(getActivity().getApplicationContext());
        sensorManager = (SensorManager) getActivity().getApplicationContext().getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
        listBluetoothDevice = new ArrayList<>();
        device_rssi_dynamic = new HashMap<String, String>();
        device_scan_no = new HashMap<String, Integer>();
        resetBLEReadings();
        resetBLEFLOORReadings();
        rotation = new float[9];
        inclination = new float[9];
        orientation = new float[3];

        View view = inflater.inflate(R.layout.fragment_navigation,
                container, false);

        textView=view.findViewById(R.id.tv_start);
        textView1=view.findViewById(R.id.textView4);

        dropdown=view.findViewById(R.id.spinner_waypoints);
        btn_nav=view.findViewById(R.id.btn_nav);
        Log.i(WEKA_TEST,  "clicked on button");

        boolean stat=scanBLE(container,view);
        return view;

    }
    private boolean scanBLE(ViewGroup container,View view) {
        System.out.println("start scanning");
        if (!scanning) {

            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    scanning = false;
                    btScanner.stopScan(leScanCallback);
                    // Log.i("Log_scan_stop", "Arraylist " + rssiArrayList);
//

                    ArrayList resultBle =  getListBluetoothDevice();
                    if(resultBle!=null && !resultBle.isEmpty()){
                        Map<String, String> resultRssiMap = getDeviceRssiDynamic();
                        Map<String, Integer> resultScanNumMap = getDeviceScanNo();

                        Log.i("My_final_call_7_p", "scan finished--" + getListBluetoothDevice() + "*" +
                                accelerometerX + "," + accelerometerY + "," + accelerometerZ +
                                "*" + magneticX + "," + magneticY + "," + magneticZ + "*" + light +
                                "*" + rotationX + "," + rotationY + "," + rotationZ + "*" +
                                orientation[0] + "," + orientation[1] + "," + orientation[2]
                        );
                        Log.i("BLE_RSSI", resultRssiMap+"");
                        Log.i("BLE_list",resultBle+"");

                        if(resultBle!= null && !resultBle.isEmpty()){
                            collectionCount++;
                            Log.i("BLE_size","resultBle="+resultBle.size()+
                                    "  resultRssiMap"+resultRssiMap.size()

                            );

                            if (resultBle.size() != 0) {
                                resetBLEReadings();
                                for (int i = 0; i < resultBle.size(); i++) {
                                    Log.i("ble" + i, resultBle.get(i) + " ");
                                    String myRssi = resultRssiMap.get(resultBle.get(i));
                                    Log.i("ble" + i, myRssi);
                                    //split the value into an array
                                    String[] arrOfStr = myRssi.split(",");
                                    int[] int_rssi_array = new int[arrOfStr.length];
                                    for (int j = 0; j < arrOfStr.length; j++) {
                                        int_rssi_array[j] = Integer.parseInt(arrOfStr[j]);
                                    }

                                    //find average of rssi or apply the filter

                                    double total = 0;
                                    for (int k = 0; k < int_rssi_array.length; k++) {
                                        total = total + int_rssi_array[k];
                                    }
                                    double average = total / int_rssi_array.length;
                                    Log.i("ble" + i, (int) average+"");
                                    Log.i("ble" + i, "--------------------------------");
                                    fixedMACBLE.put(resultBle.get(i) + "", (int) average);

                                }
                                Log.i("c_floor_check", fixedMACBLE + "");

                                int current_floor=checkTheFloor();
                                Log.i("c_floor_check", "You are on floor : "+current_floor);

                                predictTheModelF1(current_floor,container,view);

                                resultBle.clear();
                                resultRssiMap.clear();
                            }
                            // resultBle.clear();
                        }
                    }else{
                        textView.setText("Searching for nearbydevice");

                        scanBLE(container, view);
                    }

                }
            }, 2500);

            scanning = true;
            Log.i("Log_scan_3", "scan finished" + scanning);
            try {
                btScanner.startScan(leScanCallback);
                //
                // ble_class.listBluetoothDevice.clear();
            } catch (Exception e) {
                Log.i("Log_scan_4", "Exception" + e);
            }


            return true;
        } else {

            scanning = false;
            Log.i("Log_scan_5", "scan finished" + scanning);
            btScanner.stopScan(leScanCallback);
            return false;
        }
    }
    private ScanCallback leScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice myDevice = result.getDevice();
            Method getUuidsMethod = null;

            Log.i("lescan", "txpower" + myDevice.getType() + "Name: " + result.getDevice().getAddress() + " --rssi:   " + result.getRssi());

            myDevice.getUuids();
            myDevice.getBluetoothClass();
            myDevice.getAddress();

            int rssi = result.getRssi();
            String resultname = result.getDevice().getAddress();
////
            if (!(resultname == null)) {
                // tv_scan.append("Name: " + result.getDevice().getName() + " --rssi:  " + rssi + "\n");
                Log.i("SCAN", "txpower" + myDevice.getType() + "Name: " + result.getDevice().getName() + " --rssi:   " + rssi);
                addBluetoothDevice(myDevice,resultname, rssi);
                //  Log.i("add_device", "Status: " + add_status);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("Log_scan_failed", "Result=" + errorCode);
        }

        private void addBluetoothDevice(BluetoothDevice device, String myDeviceAddress, int rssi) {
            if(fixedMACBLE.containsKey(device.toString())){
                if (!listBluetoothDevice.contains(device.toString())) {

                    listBluetoothDevice.add(device.toString());
                    Log.i("Log_scan_add_0.1", device.toString() + "---" + rssi);
                    device_rssi_dynamic.put(device.toString(), Integer.toString(rssi));
                    device_scan_no.put(device.toString(), 1);


                    Log.i("Log_scan_add_1", "" + device_rssi_dynamic);

                } else {
                    Log.i("Log_scan_1_ELSE", listBluetoothDevice + "--" + device_rssi_dynamic);
                    if (device_rssi_dynamic.containsKey(device.toString())) {
                        rssi_old = device_rssi_dynamic.get(device.toString());

                        String rssi_concat = rssi_old.concat(",");
                        rssi_concat += Integer.toString(rssi);
                        device_rssi_dynamic.put(device.toString(), rssi_concat);
                        scan_val = device_scan_no.get(device.toString());
                        scan_val++;
                        device_scan_no.put(device.toString(), scan_val);
                        Log.i("Log_scan_LEUSER", device_scan_no.get(device.toString()) + "--" + rssi + "---->>>" + device_rssi_dynamic);

                    } else {
                        Log.i("Log_scan_", "no device found");

                    }

                }
            }


        }
    };
    public Map<String,String> getDeviceRssiDynamic(){
        if(!device_rssi_dynamic.isEmpty()){
            Log.i("getDeviceRssiDynamic",""+device_rssi_dynamic);
            return device_rssi_dynamic;
        }else{
            return null;
        }

    }
    public Map<String,Integer> getDeviceScanNo(){
        if(!device_rssi_dynamic.isEmpty()){
            Log.i("getDeviceScanNo",""+device_scan_no);
            return device_scan_no;
        }else{
            return null;
        }

    }
    public ArrayList<String> getListBluetoothDevice(){
        if(!listBluetoothDevice.isEmpty()){
            Log.i("getListBluetoothDevice",""+listBluetoothDevice);
            return listBluetoothDevice;
        }else{
            return null;
        }

    }
    private int checkTheFloor() {
        // Create a list from elements of HashMap
        String selected_ble = null;
        Integer value = 0;
        Map<String, Integer> map = new HashMap<String, Integer>();
        for(Map.Entry<String, Integer> entry: fixedMACBLE.entrySet()) {

            // if give value is equal to value from entry
            // print the corresponding key
            if(entry.getValue() != value) {
                map.put(entry.getKey(),entry.getValue());
            }
        }
        int maxValueInMap=(Collections.max(map.values()));  // This will return max value in the HashMap
        Log.i("c_floor_check", "Maximum value : "+maxValueInMap);

        for (Map.Entry<String, Integer> entry : fixedMACBLE.entrySet()) {  // Iterate through HashMap
            if (entry.getValue()==maxValueInMap) {
                selected_ble=entry.getKey();
                break;// Print the key with max value
            }
        }

        Log.i("c_floor_check", "Selected ble : "+selected_ble);
        return BLE_FLOOR.get(selected_ble);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // return inflater.inflate(R.layout.fragment_navigation, container, false);
    }
    private void predictTheModelF1(int r_no, ViewGroup container, View view) {
        try {
            int room_id_no=dbmanager.fetchNearByRoom(r_no,1);
            int room_no=dbmanager.fetchMyRoom(room_id_no);
            if(room_id_no==-1)
                textView.setText("Please move to a nearby accesspoint");
            else
                textView.setText(room_no+" - "+dbmanager.fetchRoomName(room_no));

            textView1.setVisibility(View.VISIBLE);
            dropdown.setVisibility(View.VISIBLE);
            btn_nav.setVisibility(View.VISIBLE);
            int[] pointsName=dbmanager.allRooms();
            Log.i("All_rooms", Arrays.toString(pointsName));
            Integer[] newArray = new Integer[pointsName.length];
            ArrayList ae = new ArrayList<>();;
            String[] rooms_with_name=new String[pointsName.length];
            int i = 0;
            for (int value : pointsName) {
                newArray[i++] = Integer.valueOf(value);
                ae.add(Integer.valueOf(value)+" - " +dbmanager.fetchRoomName(Integer.valueOf(value)));

            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(container.getContext(), android.R.layout.simple_spinner_item, ae);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dropdown.setAdapter(adapter);

            btn_nav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    SecondFragment llf = new SecondFragment();
                    Bundle args = new Bundle();
                    args.putString("spinnerValue", dropdown.getSelectedItem().toString());
                    args.putInt("start_point",room_no);

                    llf.setArguments(args);
                    ft.replace(R.id.first_fragment, llf);

                    ft.commit();
                }
            });

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void resetBLEReadings() {
        fixedMACBLE.clear();
        fixedMACBLE.put("AC:67:B2:3C:C6:46",0);//esp32 1
        fixedMACBLE.put("3C:61:05:14:A7:C2",0);//esp32 2
        fixedMACBLE.put("3C:61:05:14:B1:BA",0);//esp32 10
        fixedMACBLE.put("3C:61:05:14:B5:0A",0);//esp32 14
        fixedMACBLE.put("3C:61:05:14:A7:72",0);//esp32 20
        fixedMACBLE.put("E0:E2:E6:0D:49:76",0); //	-ESP21 -[05]
        fixedMACBLE.put("E0:E2:E6:0D:39:FA",0);	//-ESP23 -[06]
        fixedMACBLE.put("E0:E2:E6:0B:7D:7A",0);	//-ESP24 -[07]

    }
    //MAc and its corresponding floor number
    private void resetBLEFLOORReadings() {
        BLE_FLOOR.clear();
        BLE_FLOOR.put("AC:67:B2:3C:C6:46",231);//esp32 10
        BLE_FLOOR.put("3C:61:05:14:A7:C2",202);//esp32 2
        BLE_FLOOR.put("3C:61:05:14:B1:BA",207);//esp32 8
        BLE_FLOOR.put("3C:61:05:14:B5:0A",204);//esp32 14
        BLE_FLOOR.put("3C:61:05:14:A7:72",000);//esp32 20
        BLE_FLOOR.put("E0:E2:E6:0D:49:76",201); //	-ESP21 -[05]
        BLE_FLOOR.put("E0:E2:E6:0D:39:FA",205);	//-ESP23 -[06]
        BLE_FLOOR.put("E0:E2:E6:0B:7D:7A",200);	//-ESP24 -[24]
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerX = sensorEvent.values[0];
                accelerometerY = sensorEvent.values[1];
                accelerometerZ= sensorEvent.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticX = sensorEvent.values[0];
                magneticY = sensorEvent.values[1];
                magneticZ = sensorEvent.values[2];
                break;
            case Sensor.TYPE_LIGHT:
                light = sensorEvent.values[0];
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                rotationX = sensorEvent.values[0];
                rotationY = sensorEvent.values[1];
                rotationZ = sensorEvent.values[2];
                break;
            default:
                break;
        }

        SensorManager.getRotationMatrix(rotation, inclination,
                new float[] {accelerometerX, accelerometerY, accelerometerZ},
                new float[] {magneticX, magneticY, magneticZ});
        orientation = SensorManager.getOrientation(rotation, orientation);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}