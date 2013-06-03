package me.shumei.open.oks.gewara;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * <p>在点击“添加任务”或“修改任务”的插件列表后，弹出配置任务的弹窗前，《一键签到》主程序会读取String.xml里的cfg_config_type字段，如果值为2，
 * 就读取本插件layout目录下的config_layout.xml布局文件并渲染出一个View加入主程序的弹出窗口。<br />
 * 在渲染出的view加入弹窗前，主程序会调用本插件的 initView(Context context, View view, String cfg) 函数来让本插件初始化这个view。
 * 参数cfg是任务的配置信息，可以根据这个配置信息初始化这个渲染出的view，也可以给view内的各个组件添加事件监听等。<br />
 * 如果当前是“添加任务”，传入的cfg的值是一个长度为0的String（注意不是null）<br />
 * 如果当前是“修改任务”，传入的cfg的值是当前任务的配置数据<br />
 * 
 * <p>在用户填写完任务配置信息，点“确定”的时候，主程序会执行本类内的 dataValidator(Context context, View view) 函数，
 * 要在此函数内获取view里各组件的值并作有效性验证，如果全部正确就返回true，否则就返回false，
 * 如果在这个函数里返回false的话，主程序那边是添加不了任务的
 * 
 * <p>在用户保存的配置信息正确通过 dataValidator(Context context, View view) 的检测后，
 * getConfigData(Context context, View view) 函数会执行，需要在此函数内对view里要保存的值提取出来，
 * 组织在一个String里面并返回给主程序，由主程序进行保存，推荐把这些值组织成一个JSON字符串，方便读取
 * 
 * @author wolforce 2013-5-16 1:17:55
 *
 */
public class ManageTask {

	/**
	 * <p>在“添加任务”和“修改任务”页面点击列表后，弹出窗口前执行此函数。
	 * 传入当前任务的配置信息给cfg，可以用这些信息对配置用的view进行初始化。
	 * 如果当前是“添加任务”，传入的cfg的值是一个长度为0的String（注意不是null）。
	 * 如果当前是“修改任务”，传入的cfg的值是当前任务的配置数据。
	 * <p>“添加任务”时因为没有配置信息，所以不会执行此函数
	 * 
	 * @param context 主程序的Context
	 * @param view 主程序从插件的config_layout.xml里渲染出的View
	 * @param cfg 任务的配置信息，“添加任务”时会传入一个长度为0的String
	 * @return 对组件进行初始化后的view
	 */
	public View initView(Context context, View view, String cfg) {
		//如果当前传入的配置数据是一个长度为0的字符串，那就没有对组件进行初始化的必要了，直接返回原view
		if (cfg.length() == 0) {
			return view;
		}
		
		try {
			RadioButton rb_normal = (RadioButton) view.findViewById(R.id.rb_normal);
			RadioButton rb_adventure = (RadioButton) view.findViewById(R.id.rb_adventure);
			JSONObject jsonObj = new JSONObject(cfg);
			int signintype = jsonObj.getInt("signintype");//0=>稳健型，1=>冒险型
			switch (signintype) {
				case 0:
					rb_normal.setChecked(true);
					break;
					
				case 1:
					rb_adventure.setChecked(true);
					break;
					
				default:
					break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return view;
	}
	
	
	/**
	 * <p><b>在“添加任务”和“修改任务”时点击弹出窗口的“确定”按钮后，此函数会执行，用以验证配置是否正确</b></p>
	 * 
	 * 用以检查用户在config_layout.xml渲染出的view里填写的数据是否正确。如果正确就需要返回true，不正确就返回false，
	 * 如果返回false，则主程序里就无法添加任务
	 * @param context 主程序的Context
	 * @param view 用config_layout.xml渲染出的view
	 * @return 如果用户填写的配置信息正确就返回true，否则就返回false
	 */
	public boolean dataValidator(Context context, View view) {
		boolean flag = false;
		//以下代码是对自定义配置界面内的两个RadioButton进行数据有效性检查，两者中必须有一个被选中才能通过检测
		//其实因为这两个RadioButton在config_layout.xml里已经有设置默认选中第一个了，所以在这根本不用再检测，直接return true就行。
		//只是为了方便大家能了解怎么对数据进行验证，所以这里进行了一些Demo性质的操作
		RadioButton rb_normal = (RadioButton) view.findViewById(R.id.rb_normal);
		RadioButton rb_adventure = (RadioButton) view.findViewById(R.id.rb_adventure);
		if (rb_normal.isChecked() || rb_adventure.isChecked()) {
			flag = true;
		}
		
		return flag;
	}
	
	
	/**
	 * <p><b>格式化、组织配置数据</b></p>
	 * 
	 * 在此函数里需要把config_layout.xml渲染出的view里值集中到一个String里并返回给主程序，让主程序来保存这些数据。
	 * 推荐把数据做成JSON字符串。
	 * @param context 主程序的Context
	 * @param view 用config_layout.xml渲染出的view
	 * @return 返回View里处理后的配置数据
	 */
	public String getConfigData(Context context, View view) {
		JSONObject jsonObj = new JSONObject();
		try {
			//获取设置的签到红包类型
			int signintype = 0;
			RadioGroup rgSigninType = (RadioGroup) view.findViewById(R.id.rgSigninType);
			switch (rgSigninType.getCheckedRadioButtonId()) {
				case R.id.rb_normal:
					signintype = 0;
					break;
					
				case R.id.rb_adventure:
					signintype = 1;
					break;
					
				default:
					break;
			}
			jsonObj.put("signintype", signintype);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String cfg = jsonObj.toString();
		return cfg;
	}
	
	
	
}
