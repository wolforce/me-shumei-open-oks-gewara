package me.shumei.open.oks.gewara;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			String ajaxLoginUrl = "http://www.gewara.com/ajax/common/asynchLogin.dhtml";//AJAX登录URL
			String captchaHashUrl = "http://www.gewara.com/getCaptchaId.xhtml";//验证码Hash串的URL
			String captchaUrl = null;
			String signUrl = "http://www.gewara.com/home/clickGetLoginPoint.xhtml?type=";//签到URL
			
			//设置登录需要提交的数据
			HashMap<String, String> postDatas = new HashMap<String, String>();
			postDatas.put("username", user);
			postDatas.put("password", pwd);
			
			//获取验证码Hash串
			//var data = {"retval":"rPCjs6BeCgnufSQv3faaa667","success":true}
			res = Jsoup.connect(captchaHashUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(ajaxLoginUrl).ignoreContentType(true).method(Method.GET).execute();
			cookies.putAll(res.cookies());
			JSONObject jsonObj = new JSONObject(res.body().replace("var data = ", ""));
			String retval = jsonObj.getString("retval");
			
			//拼接验证码URL，然后拉取图片
			captchaUrl = "http://www.gewara.com/captcha.xhtml?captchaId=" + retval;
			if(CaptchaUtil.showCaptcha(captchaUrl , UA_CHROME, cookies, "格瓦拉生活网", user, "登录需要验证码"))
			{
				if(CaptchaUtil.captcha_input.length() > 0)
				{
					postDatas.put("captcha", CaptchaUtil.captcha_input);//输入的验证码
					postDatas.put("captchaId", retval);//验证码Hash
				}
				else
				{
					this.resultFlag = "false";
					this.resultStr = "用户放弃输入验证码，登录失败";
					return new String[]{this.resultFlag,this.resultStr};
				}
			}
			else
			{
				this.resultFlag = "false";
				this.resultStr = "拉取验证码失败，无法登录";
				return new String[]{this.resultFlag,this.resultStr};
			}
			
			//提交登录信息
			//var data = {"errorMap":{"captcha":"验证码错误！"},"success":false}
			//var data = {"id":40433108,"realname":"家前李树","success":true,"isMobile":false}
			res = Jsoup.connect(ajaxLoginUrl).data(postDatas).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(ajaxLoginUrl).ignoreContentType(true).method(Method.POST).execute();
			cookies.putAll(res.cookies());
			jsonObj = new JSONObject(res.body().replace("var data = ", ""));
			boolean success = jsonObj.getBoolean("success");
			if(success)
			{
				//登录成功
				//0=>稳赚型(稳赚2积分),1=>冒险型(随机领取-5-5积分，有几率领取100积分)
				JSONObject cfgJsonObj = new JSONObject(cfg);
				int cfgSignintype = cfgJsonObj.getInt("signintype");
				String signType = "";
				if (cfgSignintype == 1) {
					signType = "bit";
				} else {
					signType = "";
				}
				signUrl = signUrl + signType;
				//提交签到请求，如果已领取过就返回Json，否则返回一个网页
				//var data = {"msg":"你今天已经领取过红包！","success":false}
				res = Jsoup.connect(signUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(ajaxLoginUrl).ignoreContentType(true).method(Method.GET).execute();
				cookies.putAll(res.cookies());
				//System.out.println(res.body());
				
				if(res.body().contains("今天已经领取"))
				{
					this.resultFlag = "true";
					this.resultStr = "你今天已经领取过红包！";
				}
				else
				{
					if(res.body().contains("成功领取") || res.body().contains("当前积分"))
					{
						/*
						<div class="get_red get_red_after">
							<p style="padding:25px 0 35px 0" class="center">
												<b class="ml20">
									今日成功领取<font color="red">5</font>个积分的大礼包！			
								</b><br/>
								<b class="ml20">
									<br /> 你当前积分总数为<a href="/home/acct/pointList.xhtml"><font style="font-size: 14px;color: red;">55</font></a>，积分可用于支付抵值，继续加油哦^_^
								</b>
											</p>
						</div>
						 */
						String result = res.parse().getElementsByClass("get_red_after").text();//从返回的网页里提取出信息
						this.resultFlag = "true";
						this.resultStr = result;
					}
					else
					{
						this.resultFlag = "false";
						this.resultStr = "登录成功但领取红包失败";
					}
				}
			}
			else
			{
				this.resultFlag = "false";
				if(res.body().contains("验证码错误"))
				{
					this.resultStr = "验证码错误！";
				}
				else
				{
					this.resultStr = "登录失败，请检查账号密码是否正确";
				}
			}
			
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
