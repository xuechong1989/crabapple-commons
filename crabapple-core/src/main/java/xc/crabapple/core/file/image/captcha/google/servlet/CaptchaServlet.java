package xc.crabapple.core.file.image.captcha.google.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xc.crabapple.core.file.image.captcha.google.GoogleCaptchaUtil;
import xc.crabapple.core.file.image.captcha.google.type.SpecCaptcha;
import xc.crabapple.core.file.image.captcha.google.base.Captcha;

import java.awt.*;
import java.io.IOException;

public final class CaptchaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String SESSION_KEY = "captcha";
    private static final int DEFAULT_LEN = 4;
    private static final int DEFAULT_WIDTH = 130;
    private static final int DEFAULT_HEIGHT = 48;

    public CaptchaServlet() {

    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        out(request, response);
    }

    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    public static final void out(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        out(4, request, response);
    }

    public static final void out(final int len, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        out(130, 48, len, request, response);
    }

    public static final void out(final int width, final int height, final int len, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        out(width, height, len, (Font)null, request, response);
    }

    public static final void out(final Font font, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        out(130, 48, 4, font, request, response);
    }

    public static final void out(final int len, final Font font, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        out(130, 48, len, font, request, response);
    }

    public static final void out(final int width, final int height, final int len, final Font font, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        SpecCaptcha specCaptcha = new SpecCaptcha(width, height, len);
        if (font != null) {
            specCaptcha.setFont(font);
        }

        out((Captcha)specCaptcha, request, response);
    }

    public static void out(Captcha captcha, HttpServletRequest request, HttpServletResponse response) throws IOException {
        GoogleCaptchaUtil.setHeader(response);
        request.getSession().setAttribute("captcha", captcha.text().toLowerCase());
        captcha.out(response.getOutputStream());
    }

    public static final boolean ver(final String code, final HttpServletRequest request) {
        if (code != null) {
            String captcha = (String)request.getSession().getAttribute("captcha");
            return code.trim().toLowerCase().equals(captcha);
        } else {
            return false;
        }
    }

    public static final void clear(final HttpServletRequest request) {
        request.getSession().removeAttribute("captcha");
    }

}