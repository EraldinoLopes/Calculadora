// Complex.java
public class Complex {

    private final double re;
    private final double im;

    public Complex(double real, double imag) {
        this.re = real;
        this.im = imag;
    }

    // --------------------------
    // ------ PARSE COMPLEX -----
    // --------------------------
    public static Complex parse(String input) {
        input = input.trim().replace(" ", "");

        if (input.equals("i")) return new Complex(0, 1);
        if (input.equals("+i")) return new Complex(0, 1);
        if (input.equals("-i")) return new Complex(0, -1);

        if (!input.contains("i")) {
            return new Complex(Double.parseDouble(input), 0);
        }

        String realPart = "";
        String imagPart = "";

        int iPos = input.indexOf("i");
        imagPart = input.substring(0, iPos);

        int lastPlus = input.lastIndexOf('+', iPos - 1);
        int lastMinus = input.lastIndexOf('-', iPos - 1);
        int splitPos = Math.max(lastPlus, lastMinus);

        if (splitPos <= 0) {
            double im;
            if (imagPart.equals("") || imagPart.equals("+"))
                im = 1;
            else if (imagPart.equals("-"))
                im = -1;
            else
                im = Double.parseDouble(imagPart);
            return new Complex(0, im);
        }

        realPart = input.substring(0, splitPos);
        imagPart = input.substring(splitPos, iPos);

        double re = Double.parseDouble(realPart);

        double im;
        if (imagPart.equals("+") || imagPart.equals(""))
            im = 1;
        else if (imagPart.equals("-"))
            im = -1;
        else
            im = Double.parseDouble(imagPart);

        return new Complex(re, im);
    }

    public double getReal() {
        return re;
    }

    public double getImag() {
        return im;
    }

    // --------------------------
    // ----- OPERAÇÕES BÁSICAS --
    // --------------------------
    public Complex plus(Complex b) {
        return new Complex(this.re + b.re, this.im + b.im);
    }

    public Complex minus(Complex b) {
        return new Complex(this.re - b.re, this.im - b.im);
    }

    public Complex times(Complex b) {
        return new Complex(
                this.re * b.re - this.im * b.im,
                this.re * b.im + this.im * b.re
        );
    }

    public Complex divide(Complex b) {
        double denom = b.re * b.re + b.im * b.im;
        return new Complex(
                (this.re * b.re + this.im * b.im) / denom,
                (this.im * b.re - this.re * b.im) / denom
        );
    }

    public Complex scale(double s) {
        return new Complex(this.re * s, this.im * s);
    }

    public Complex conjugate() {
        return new Complex(this.re, -this.im);
    }

    public double abs() {
        return Math.hypot(re, im);
    }

    // --------------------------
    // -------- POTÊNCIA --------
    // --------------------------
    public Complex pow(double x) {
        double r = this.abs();
        double theta = Math.atan2(im, re);

        double newR = Math.pow(r, x);
        double newTheta = theta * x;

        return new Complex(
                newR * Math.cos(newTheta),
                newR * Math.sin(newTheta)
        );
    }

    // --------------------------
    // ----- FUNÇÕES COMPLEXAS --
    // --------------------------
    public static Complex exp(Complex z) {
        double expReal = Math.exp(z.re);
        return new Complex(
                expReal * Math.cos(z.im),
                expReal * Math.sin(z.im)
        );
    }

    public static Complex log(Complex z) {
        double r = z.abs();
        double theta = Math.atan2(z.im, z.re);
        return new Complex(Math.log(r), theta);
    }

    public static Complex sin(Complex z) {
        return new Complex(
                Math.sin(z.re) * Math.cosh(z.im),
                Math.cos(z.re) * Math.sinh(z.im)
        );
    }

    public static Complex cos(Complex z) {
        return new Complex(
                Math.cos(z.re) * Math.cosh(z.im),
                -Math.sin(z.re) * Math.sinh(z.im)
        );
    }

    public static Complex tan(Complex z) {
        return sin(z).divide(cos(z));
    }

    public static Complex sqrt(double x) {
        if (x < 0) {
            return new Complex(0, Math.sqrt(-x));
        }
        return new Complex(Math.sqrt(x), 0);
    }

    @Override
    public String toString() {
        if (im == 0) return String.valueOf(re);
        if (re == 0) return im + "i";
        if (im < 0) return re + "" + im + "i";
        return re + "+" + im + "i";
    }
}