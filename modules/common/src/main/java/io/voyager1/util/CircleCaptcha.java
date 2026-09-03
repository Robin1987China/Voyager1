/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.util;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 圆圈干扰验证码 {@code io.voyager1.util.CircleCaptcha}。
 */
public class CircleCaptcha {

    protected int width;
    protected int height;
    protected int interfereCount;
    protected Font font;
    protected String code;
    protected byte[] imageBytes;
    protected CodeGenerator generator;
    protected Color background = Color.WHITE;
    protected AlphaComposite textAlpha;
    protected Stroke stroke;

    public CircleCaptcha(int width, int height) {
        this(width, height, 5);
    }

    public CircleCaptcha(int width, int height, int codeCount) {
        this(width, height, codeCount, 15);
    }

    public CircleCaptcha(int width, int height, int codeCount, int interfereCount) {
        this(width, height, new RandomGenerator(codeCount), interfereCount);
    }

    public CircleCaptcha(int width, int height, CodeGenerator generator, int interfereCount) {
        this.width = width;
        this.height = height;
        this.generator = generator;
        this.interfereCount = interfereCount;
        this.font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (this.height * 0.75));
    }

    /**
     * 生成验证码字符串并绘制图片。
     */
    public void createCode() {
        generateCode();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write((BufferedImage) createImage(this.code), "png", out);
            this.imageBytes = out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成验证码图片失败", e);
        }
    }

    protected void generateCode() {
        this.code = generator.generate();
    }

    /**
     * 获取验证码字符串，若未生成则先生成。
     */
    public String getCode() {
        if (this.code == null) {
            createCode();
        }
        return this.code;
    }

    /**
     * 校验用户输入的验证码。
     */
    public boolean verify(String userInputCode) {
        return this.generator.verify(getCode(), userInputCode);
    }

    public void write(String path) {
        write(new File(path));
    }

    public void write(File file) {
        try (OutputStream out = new java.io.FileOutputStream(file)) {
            write(out);
        } catch (IOException e) {
            throw new IllegalStateException("写出验证码图片失败", e);
        }
    }

    public void write(OutputStream out) {
        try {
            out.write(getImageBytes());
        } catch (IOException e) {
            throw new IllegalStateException("写出验证码图片失败", e);
        }
    }

    public byte[] getImageBytes() {
        if (this.imageBytes == null) {
            createCode();
        }
        return this.imageBytes;
    }

    public BufferedImage getImage() {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(getImageBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("读取验证码图片失败", e);
        }
    }

    public String getImageBase64() {
        return Base64.getEncoder().encodeToString(getImageBytes());
    }

    /**
     * 获取带 data URI 前缀的 Base64 图片数据。
     */
    public String getImageBase64Data() {
        return "data:image/png;base64," + getImageBase64();
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public CodeGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(CodeGenerator generator) {
        this.generator = generator;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public void setTextAlpha(float textAlpha) {
        this.textAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha);
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    /**
     * 根据验证码绘制图片。
     */
    protected java.awt.Image createImage(String code) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            if (background != null) {
                g.setColor(background);
                g.fillRect(0, 0, width, height);
            }
            drawInterfere(g);
            drawString(g, code);
        } finally {
            g.dispose();
        }
        return image;
    }

    protected void drawString(Graphics2D g, String code) {
        if (code == null || code.isEmpty()) {
            return;
        }
        if (textAlpha != null) {
            g.setComposite(textAlpha);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int charCount = code.length();
        int charWidth = width / Math.max(1, charCount + 1);
        for (int i = 0; i < charCount; i++) {
            g.setFont(font);
            g.setColor(randomColor(random));
            int x = charWidth * (i + 1) - (charWidth >> 1);
            int y = height / 2 + (font == null ? height / 4 : font.getSize() / 2) - random.nextInt(4);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
    }

    protected void drawInterfere(Graphics2D g) {
        if (stroke != null) {
            g.setStroke(stroke);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < interfereCount; i++) {
            g.setColor(randomColor(random));
            int r = Math.max(1, random.nextInt(height >> 1));
            g.drawOval(random.nextInt(width), random.nextInt(height), r, r);
        }
    }

    private static Color randomColor(ThreadLocalRandom random) {
        return new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }
}
