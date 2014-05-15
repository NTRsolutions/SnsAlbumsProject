
package bluetooth;

import java.util.Set;

import album.entry.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


/**
 * @ClassName: DeviceListActivity 
 * @Description:  * 	
 * 		1. 显示可发现的蓝牙设备
 * 		2. 显示已经配对的设备
 *  	3. 扫描设备
 *  	4. 使设备可发现
 * @Author: Mr.Simple (何红辉)
 * @E-mail: bboyfeiyu@gmail.com 
 * @Date 2012-11-18 下午6:51:11 
 *
 */

public class DeviceListActivity extends Activity {
	
    // 调试信息
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // 返回的Intent的额外信息
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // 数据成员
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    
    /**
     * 功能 ： 页面创建,初始化控件和变量等
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        setResult(Activity.RESULT_CANCELED);

        // 扫描按钮
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // 初始化适配器数组
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // 为已配对的设备listView设置适配器和点击事件
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // 新发现的设备列表
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // 当扫描完设备后注册一个接收器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // 当查找蓝牙设备结束后注册一个广播接收器
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // 获取蓝牙适配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // 蓝牙设备的集合
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // 如果有已配对设备,则添加到mPairedDevicesArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    /**
     * 功能： 启动扫描设备
     * 
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // 指定页面标题
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // 如果正在扫描.则停止
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // 适配器启动扫描
        mBtAdapter.startDiscovery();
    }

    /**
     * 功能：  在列表上的点击事件.
     * 
     */
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
    
            mBtAdapter.cancelDiscovery();

            // 获取MAC地址,最后17位字符为MAC地址
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // 进行连接,传递蓝牙的MAC地址
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


    /**
     *  功能： 广播接收器,当扫描完毕后监听发现设备的和改变页面标题
     *  
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 当扫描发现了一个设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            	
                // 从Intent中获取BluetoothDevice对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果已经配对,则跳过
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                
            // 当扫描结束,改变页面标题
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
