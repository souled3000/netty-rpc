package com.blackcrystalinfo.platform.captcha;

import java.awt.Font;

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
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.image.gimpy.GimpyFactory;

public class MyImageCaptchaEngine extends ListImageCaptchaEngine {
	protected void buildInitialFactories() {
		WordGenerator wgen = new RandomWordGenerator("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
		// WordGenerator wgen = new RandomWordGenerator("李春江");
		RandomRangeColorGenerator cgen = new RandomRangeColorGenerator(new int[] { 255, 255 }, new int[] { 255, 255 }, new int[] { 255, 255 });
		TextPaster textPaster = new RandomTextPaster(new Integer(7), new Integer(7), cgen, true);

		BackgroundGenerator backgroundGenerator = new FunkyBackgroundGenerator(new Integer(70), new Integer(30));

		Font[] fontsList = new Font[] { new Font("Arial", 0, 8), new Font("Tahoma", 0, 8), new Font("Verdana", 0, 8), };

		FontGenerator fontGenerator = new RandomFontGenerator(new Integer(14), new Integer(14), fontsList);

		WordToImage wordToImage = new ComposedWordToImage(fontGenerator, backgroundGenerator, textPaster);
		
		this.addFactory(new GimpyFactory(wgen, wordToImage));
	}
}