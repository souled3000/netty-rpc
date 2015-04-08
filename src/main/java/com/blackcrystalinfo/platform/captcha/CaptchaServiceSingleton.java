package com.blackcrystalinfo.platform.captcha;

import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
 
public class CaptchaServiceSingleton {
 
//    private static ImageCaptchaService instance = new DefaultManageableImageCaptchaService();
    private static ImageCaptchaService instance;
    static {
        instance = new DefaultManageableImageCaptchaService(
            new FastHashMapCaptchaStore()/**缓冲池*/,
            new MyImageCaptchaEngine()/**验证码图片的样子*/,
            0/**保证的最小的延迟单位秒*/,
            100000/**缓冲池大小*/,
            75000/**垃圾回收前存储负载*/);
    }
    public static ImageCaptchaService getInstance(){
        return instance;
    }
    
}