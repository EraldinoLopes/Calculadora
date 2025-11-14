import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Complex {
    private final double real;
    private final double imag;

    public Complex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    public double getReal() { return real; }
    public double getImag() { return imag; }


    public Complex plus(Complex b) {
        return new Complex(real + b.real, imag + b.imag);
    }

    public Complex minus(Complex b) {
        return new Complex(real - b.real, imag - b.imag);
    }

    public Complex times(Complex b) {
        double novoReal = (real * b.real) - (imag * b.imag);
        double novoImag = (real * b.imag) + (imag * b.real);
        return new Complex(novoReal, novoImag);
    }

    public Complex scale(double alpha) {
        return new Complex(real * alpha, imag * alpha);
    }

    public Complex divide(Complex b) {
        double divisor = (b.real * b.real) + (b.imag * b.imag);
        if (divisor == 0.0) {
            throw new ArithmeticException("Divisão por zero no número complexo.");
        }
        Complex numerador = this.times(b.conjugate());
        return new Complex(numerador.real / divisor, numerador.imag / divisor);
    }

    public Complex conjugate() {
        return new Complex(real, -imag);
    }


    public Complex pow(double exponent) {
        if (real == 0 && imag == 0 && exponent > 0) return new Complex(0, 0);

        double r = Math.sqrt(real * real + imag * imag);
        double theta = Math.atan2(imag, real);

        double novoR = Math.pow(r, exponent);
        double novoTheta = theta * exponent;

        double novoReal = novoR * Math.cos(novoTheta);
        double novoImag = novoR * Math.sin(novoTheta);

        return new Complex(novoReal, novoImag);
    }

    public static Complex sqrt(double x){
        if(x >= 0){
            return new Complex(Math.sqrt(x), 0);
        }else{
            return new Complex(0, Math.sqrt(-x));
        }
    }


    private static double parseComponentValue(String s) {
        s = s.replaceAll("\\s+", "");
        if (s.isEmpty()) return 0.0;

        String numStr = s.replaceAll("[iI]", "");

        if (numStr.isEmpty()) {
            if (s.contains("+")) return 1.0;
            if (s.contains("-")) return -1.0;
            return 1.0;
        }

        if (numStr.equals("+")) return 1.0;
        if (numStr.equals("-")) return -1.0;

        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Componente numérico inválido: " + s);
        }
    }


    public static Complex parse(String s) {
        String cleanS = s.replaceAll("\\s+", "");
        if (cleanS.isEmpty()) throw new IllegalArgumentException("Entrada vazia.");

        if (cleanS.endsWith("i") || cleanS.endsWith("I")) {
            if (cleanS.indexOf('+') == -1 && cleanS.indexOf('-', 1) == -1) {
                double imag = parseComponentValue(cleanS);
                return new Complex(0.0, imag);
            }
        }

        if (!cleanS.contains("i") && !cleanS.contains("I")) {
            try {
                double real = Double.parseDouble(cleanS);
                return new Complex(real, 0.0);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Formato inválido: " + s);
            }
        }

        int splitIndex = -1;
        for (int i = 1; i < cleanS.length(); i++) {
            char c = cleanS.charAt(i);
            if ((c == '+' || c == '-') && (cleanS.charAt(i-1) != 'e' && cleanS.charAt(i-1) != 'E')) {
                splitIndex = i;
                break;
            }
        }

        String realStr, imagStr;
        if (splitIndex != -1) {
            realStr = cleanS.substring(0, splitIndex);
            imagStr = cleanS.substring(splitIndex);
        } else {
            throw new IllegalArgumentException("Formato do número complexo inválido: " + s);
        }

        double realPart = parseComponentValue(realStr);
        double imagPart = parseComponentValue(imagStr);

        return new Complex(realPart, imagPart);
    }


    @Override
    public String toString() {
        if (Math.abs(imag) < 1e-9) {
            return String.format("%.4f", real);
        }
        if (Math.abs(real) < 1e-9) {
            if (Math.abs(imag - 1.0) < 1e-9) return "i";
            if (Math.abs(imag + 1.0) < 1e-9) return "-i";
            return String.format("%.4fi", imag);
        }

        if (imag < 0) {
            return String.format("%.4f - %.4fi", real, -imag);
        }
        return String.format("%.4f + %.4fi", real, imag);
    }
}