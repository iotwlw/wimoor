package com.wimoor.common.mvc;


import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;


import org.apache.commons.net.ftp.FTPClient;


public class FileUpload {  

  public static final String FILE_PATH = "/upload/";  

  public static File getFile(String fileName) {  
      return new File(FILE_PATH, fileName);  
  }  
  
  public static String getPictureImage(String value) {
		if(value!=null&&!value.contains("http")&&(value.contains("photo/")||value.contains("wimoor-file/"))) {
			//value=value.replace("photo/","https://wimoor-file.oss-cn-shenzhen.aliyuncs.com/old/");
			value=value.replace("photo/","https://photo.wimoor.com/");
			value=value.replace("%2B","%252B");
			value=value.replace("wimoor-file/", "https://wimoor-file.oss-cn-shenzhen.aliyuncs.com/");
		}else if(value==null) {
	    	value="https://wimoor-file.oss-cn-shenzhen.aliyuncs.com/sys/photos/noimg.png";
	    } 
		return value;
  }
 
  
  public static List<Map<String, Object>> covertPictureImage(List<Map<String, Object>> list) {
	   for(Map<String, Object> item:list) {
		   item.put("image",FileUpload.getPictureImage(item.get("image")));
		   item.put("location",FileUpload.getPictureImage(item.get("location")));
	   }
	   return list;
  }
  
  public static String getPictureImage(Object value) {
	  String result=null;
	  if(value!=null)result=value.toString();
	  return getPictureImage(result);
  }
  
	public static Boolean deletePicture(String path) {
		FTPServerUtil ftputil=new FTPServerUtil();
		try {
			FTPClient ftpClient = ftputil.getFTPClient();
			if(path == null) return false;
			if(path.indexOf("photo/")==0) {
				path=path.substring(6,path.length());
			}
			String[] patharray = path.split("/");
			if(patharray.length>0) {
				path=patharray[0];
				for(int i=1;i<patharray.length-1;i++) {
					path=path+"/"+patharray[i];
				}
				return ftputil.deleteFile(ftpClient, path, patharray[patharray.length-1]);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("图片操作失败");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("图片操作失败");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("图片操作失败");
		}
		
		return false;
	}
} 