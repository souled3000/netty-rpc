package com.blackcrystalinfo.platform.captcha;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Properties;

import com.blackcrystalinfo.platform.util.Constants;
import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.FunkyBackgroundGenerator;
import com.octo.captcha.component.image.color.RandomRangeColorGenerator;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.RandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;

public final class Captcha {
	private static String dictionary;
	private static int length;
    private static WordToImage wordToImage;
    private static WordGenerator wordGenerator;
    public static int expire;
    public static boolean validity = false;
	static{
		Properties p = new Properties();
		try {
			p.load(ClassLoader.getSystemResourceAsStream("captcha.properties"));
			dictionary = p.getProperty("dictionary");
			length = Integer.parseInt(p.getProperty("length"));
			wordGenerator = new RandomWordGenerator(dictionary);
			
			RandomRangeColorGenerator cgen = new RandomRangeColorGenerator(new int[] { 255, 255 }, new int[] { 255, 255 }, new int[] { 255, 255 });
			
			TextPaster textPaster = new RandomTextPaster(new Integer(length), new Integer(length), cgen, true);

			BackgroundGenerator backgroundGenerator = new FunkyBackgroundGenerator(new Integer(70), new Integer(30));

			Font[] fontsList = new Font[] { new Font("Arial", 0, 8), new Font("Tahoma", 0, 8), new Font("Verdana", 0, 8), };

			FontGenerator fontGenerator = new RandomFontGenerator(new Integer(14), new Integer(14), fontsList);

			wordToImage = new ComposedWordToImage(fontGenerator, backgroundGenerator, textPaster);
			
			expire = Constants.CAPTCHA_EXPIRE;
			validity = Boolean.parseBoolean(p.getProperty("validity","false"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static BufferedImage getImage(String word){
		return wordToImage.getImage(word);
	}
	
	public static String getWord(){
		return wordGenerator.getWord(length);
	}
}
