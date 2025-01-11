import java.io.File;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class FFTExample {

    public static void main(String[] args) throws Exception {
        File inputFile = new File("C:\\Users\\Dr Rajesh Badani\\Desktop\\audio.wav");
        File outputFile = new File("output.wav");

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat format = audioInputStream.getFormat();
        byte[] audioBytes = audioInputStream.readAllBytes();

        double[] audioData = convertBytesToDouble(audioBytes);

        // Find the next power of 2 greater than or equal to audioData length
        int nextPowerOf2 = findNextPowerOf2(audioData.length);
        double[] paddedAudioData = Arrays.copyOf(audioData, nextPowerOf2);

        // Apply FFT
        Complex[] frequencyData = fft(paddedAudioData);

        // Process frequencyData if needed...

        // Apply inverse FFT
        double[] processedAudioData = ifft(frequencyData);

        // Convert processed data back to byte array
        byte[] processedAudioBytes = convertDoubleToBytes(processedAudioData, format);

        // Write the processed audio data to output file
        try (AudioInputStream outputAudioInputStream = new AudioInputStream(
                new ByteArrayInputStream(processedAudioBytes), format, processedAudioData.length)) {
            AudioSystem.write(outputAudioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        }
    }

    private static double[] convertBytesToDouble(byte[] audioBytes) {
        int length = audioBytes.length / 2;
        double[] audioData = new double[length];
        for (int i = 0; i < length; i++) {
            int LSB = audioBytes[2 * i] & 0xff;
            int MSB = audioBytes[2 * i + 1] << 8;
            audioData[i] = (short) (MSB | LSB) / 32768.0;
        }
        return audioData;
    }

    private static byte[] convertDoubleToBytes(double[] audioData, AudioFormat format) {
        byte[] audioBytes = new byte[audioData.length * 2];
        for (int i = 0; i < audioData.length; i++) {
            int temp = (int) (audioData[i] * 32768);
            audioBytes[2 * i] = (byte) (temp & 0x00FF);
            audioBytes[2 * i + 1] = (byte) ((temp & 0xFF00) >> 8);
        }
        return audioBytes;
    }

    private static Complex[] fft(Complex[] input) {
        int n = input.length;

        // Base case: if the input length is 1, just return the input
        if (n == 1) {
            return new Complex[]{input[0]};
        }

        // Check if the input length is a power of 2
        if (n % 2 != 0) {
            throw new IllegalArgumentException("Input length must be a power of 2");
        }

        // Divide input into even and odd parts
        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = input[i * 2];
            odd[i] = input[i * 2 + 1];
        }

        // Recursively apply FFT to even and odd parts
        Complex[] fftEven = fft(even);
        Complex[] fftOdd = fft(odd);

        // Combine the results
        Complex[] output = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            Complex t = fftOdd[k].multiply(Complex.fromPolar(1, -2 * Math.PI * k / n));
            output[k] = fftEven[k].add(t);
            output[k + n / 2] = fftEven[k].subtract(t);
        }

        return output;
    }

    private static double[] ifft(Complex[] input) {
        int n = input.length;

        // Conjugate the complex numbers
        Complex[] conjugated = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjugated[i] = input[i].conjugate();
        }

        // Apply FFT to the conjugated array
        Complex[] fftConjugated = fft(conjugated);

        // Conjugate again and scale by 1/n
        double[] output = new double[n];
        for (int i = 0; i < n; i++) {
            output[i] = fftConjugated[i].conjugate().real / n;
        }

        return output;
    }

    private static Complex[] fft(double[] input) {
        int n = input.length;
        Complex[] complexInput = new Complex[n];

        for (int i = 0; i < n; i++) {
            complexInput[i] = new Complex(input[i], 0);
        }

        return fft(complexInput);
    }

    private static int findNextPowerOf2(int length) {
        int power = 1;
        while (power < length) {
            power *= 2;
        }
        return power;
    }

    static class Complex {
        double real;
        double imaginary;

        Complex(double real, double imaginary) {
            this.real = real;
            this.imaginary = imaginary;
        }

        Complex add(Complex other) {
            return new Complex(this.real + other.real, this.imaginary + other.imaginary);
        }

        Complex subtract(Complex other) {
            return new Complex(this.real - other.real, this.imaginary - other.imaginary);
        }

        Complex multiply(Complex other) {
            return new Complex(this.real * other.real - this.imaginary * other.imaginary,
                    this.real * other.imaginary + this.imaginary * other.real);
        }

        Complex conjugate() {
            return new Complex(this.real, -this.imaginary);
        }

        static Complex fromPolar(double magnitude, double angle) {
            return new Complex(magnitude * Math.cos(angle), magnitude * Math.sin(angle));
        }
    }
}
