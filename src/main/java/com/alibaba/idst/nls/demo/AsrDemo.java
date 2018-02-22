package com.alibaba.idst.nls.demo;

import java.io.*;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.NlsFuture;
import com.alibaba.idst.nls.event.NlsEvent;
import com.alibaba.idst.nls.event.NlsListener;
import com.alibaba.idst.nls.protocol.NlsRequest;
import com.alibaba.idst.nls.protocol.NlsResponse;

public class AsrDemo implements NlsListener {
	private static NlsClient client = new NlsClient();
	private static String akId = "LTAIty94C5TYbKW9";
	private static String akSecret = "KGPr0z0ACr4cL6SiYazvH6et4ROEM3";
	private static String begin_path="/home/buddy/ASR/ASR_tool/0825data/";


	private static String type_path="5m_zhegnchang_shibie";
//	private static String type_path="5m_anjing_shibie";
	//private static String type_path="3m_zhengchang_shibie";
	//private static String type_path="2m_zhngchang_shibie/";
	//private static String type_path="2m_anjing_shibie/";
	//private static String type_path="2m_dazao_shibie/";`


	private static String result_destination="/home/buddy/ASR/ASR_tool/asr_tool/speech_test/ali_"+type_path+".txt";
	private static String filename_temp="";



	public AsrDemo(String akId, String akSecret) {
		System.out.println("init Nls client...");
		this.akId = akId;
		this.akSecret = akSecret;
		// 初始化NlsClient
		client.init();
	}

	public void shutDown() {
		System.out.println("close NLS client");
		// 关闭客户端并释放资源
		client.close();
		System.out.println("demo done");
	}

	public void startAsr(String full_file_name) {
		//开始发送语音
		System.out.println("open audio file...");
        InputStream fis = null;
        if(!full_file_name.endsWith(".pcm")){
        	return;
		}
        try {
            fis = new FileInputStream(new File(full_file_name));
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (fis != null) {
			System.out.println("create NLS future");
			try {
				NlsRequest req = new NlsRequest();
				req.setAppKey("nls-service"); // appkey请从 "快速开始" 帮助页面的appkey列表中获取
                req.setAsrFormat("pcm"); // 设置语音文件格式为pcm,我们支持16k 16bit 的无头的pcm文件。

				/*热词相关配置*/
				//req.setAsrVocabularyId("热词词表id");//热词词表id
				/*热词相关配置*/


				req.authorize(akId, akSecret); // 请替换为用户申请到的Access Key ID和Access Key
				// Secret
				NlsFuture future = client.createNlsFuture(req, this); // 实例化请求,传入请求和监听器
				System.out.println("call NLS service");
				byte[] b = new byte[8000];
				int len = 0;
				while ((len = fis.read(b)) > 0) {
					future.sendVoice(b, 0, len); // 发送语音数据
					Thread.sleep(50);
				}
				future.sendFinishSignal(); // 语音识别结束时，发送结束符
				System.out.println("main thread enter waiting for less than 10s.");
				future.await(10000); // 设置服务端结果返回的超时时间
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("calling NLS service end");
		}
	}

	@Override
	public void onMessageReceived(NlsEvent e) {
		//识别结果的回调
		NlsResponse response = e.getResponse();
		String result = "";
		int statusCode = response.getStatus_code();
		if (response.getAsr_ret() != null) {
			result += "\nget asr result: statusCode=[" + statusCode + "], " + response.getAsr_ret();
		}
		if (result != null) {
			System.out.println(result);
			if (response.getAsr_ret()!=null&&!"".equals(response.getAsr_ret())){

				JSONObject json = (JSONObject)JSONObject.parse(response.getAsr_ret());
				String result_text = (String)json.get("result");

				System.out.println("========================="+result_text);
				PrintWriter pw=null;
				try {
					 pw=new PrintWriter(new FileOutputStream(new File(result_destination),true),true);

					pw.println(filename_temp+" "+result_text);

				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}finally {
					if (pw !=null) {
						pw.close();
					}
				}

			}
		} else {
			System.out.println(response.jsonResults.toString());
		}
	}

	@Override
	public void onOperationFailed(NlsEvent e) {
		//识别失败的回调
		String result = "";
		result += "on operation failed: statusCode=[" + e.getResponse().getStatus_code() + "], " + e.getErrorMessage();
		System.out.println(result);
	}

	@Override
	public void onChannelClosed(NlsEvent e) {
		//socket 连接关闭的回调
		System.out.println("on websocket closed.");
	}

	public static void main(String[] args) {

		AsrDemo asrDemo = new AsrDemo(akId, akSecret);


		File file_path=new File(begin_path+type_path+"/");





		for (int i = 0; i < file_path.list().length; i++) {

			String file_name=file_path.list()[i];
			System.out.println(file_name);
			filename_temp=file_name;

			asrDemo.startAsr(file_path+"/"+file_name);
		}




		asrDemo.shutDown();







	}
}
