package convertor;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.*;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bytedeco.javacpp.avcodec.*;

/**
 * @author LiaoQinZhou
 * @date: 2021/1/27 08:57
 */
public class RtmpTpRtmp2 {

    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    //ץȡ��
    FFmpegFrameGrabber grabber = null;
    //������
    FFmpegFrameRecorder record = null;

    // ��Ƶ����
    int width = -1, height = -1;
    protected int audiocodecid;
    protected int codecid;
    protected double framerate;// ֡��
    protected int bitrate;// ������

    // ��Ƶ����
    // ��Ҫ¼����Ƶ�����������������У�audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
    private int audioChannels;
    private int audioBitrate;
    private int sampleRate;

    /**
     * ѡ����ƵԴ
     *
     * @param src
     * @throws Exception
     * @author eguid
     */
    public RtmpTpRtmp2 from(String src) throws Exception {
        // �ɼ�/ץȡ��
        grabber = new FFmpegFrameGrabber(src);
        if (src.indexOf("rtsp") >= 0) {
            grabber.setOption("rtsp_transport", "tcp");
        }
        grabber.start();// ��ʼ֮��ffmpeg��ɼ���Ƶ��Ϣ��֮��Ϳ��Ի�ȡ����Ƶ��Ϣ
        if (width < 0 || height < 0) {
            width = grabber.getImageWidth();
            height = grabber.getImageHeight();
        }
        // ��Ƶ����
        audiocodecid = grabber.getAudioCodec();
        codecid = grabber.getVideoCodec();
        framerate = grabber.getVideoFrameRate();// ֡��
        bitrate = grabber.getVideoBitrate();// ������
        // ��Ƶ����
        audioChannels = grabber.getAudioChannels();
        audioBitrate = grabber.getAudioBitrate();
        if (audioBitrate < 1) {
            audioBitrate = 128 * 1000;// Ĭ����Ƶ������
        }
        sampleRate = grabber.getSampleRate();
        return this;
    }

    /**
     * ѡ�����
     *
     * @param out
     * @throws IOException
     */
    public RtmpTpRtmp2 to(String out) throws IOException {
        // ¼��/������
        record = new FFmpegFrameRecorder(out, width, height);
        // ת�����
        record.setVideoOption("crf", "18");
        // ������
        record.setGopSize(2);
        // ֡��
        record.setFrameRate(framerate);
        //��Ƶ����
        record.setVideoBitrate(bitrate);
        record.setAudioChannels(audioChannels);
        record.setAudioBitrate(audioBitrate);
        record.setSampleRate(sampleRate);
        //����ffmpeg�ṹ��
        /**
         * eg:
         * AVIOContext *pb���������ݵĻ���
         * unsigned int nb_streams������Ƶ���ĸ���
         * AVStream **streams������Ƶ��
         * char filename[1024]���ļ���
         * int64_t duration��ʱ������λ��΢��us��ת��Ϊ����Ҫ����1000000��
         * int bit_rate�������ʣ���λbps��ת��Ϊkbps��Ҫ����1000��
         * AVDictionary *metadata��Ԫ����
         */
        //avformat.AVFormatContext fc = null;
        if (out.indexOf("rtmp") >= 0 || out.indexOf("flv") > 0) {
            //������Ƶ�ļ������ʽ
            record.setVideoCodec(AV_CODEC_ID_H264);
            record.setAudioCodec(AV_CODEC_ID_AAC);
            // ��װ��ʽflv
            record.setFormat("flv");
            //fc = grabber.getFormatContext();
        }
        //record.start(fc);
        record.start();

        //����swing����
        CanvasFrame canvas = new CanvasFrame("camera", CanvasFrame.getDefaultGamma());
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        //�����̴߳�������֡
        pool.execute(() -> {
            try {
                Frame frame = null;
                //ѭ�������ڲ��رվͼ���ץȡ
                while (canvas.isVisible()
                        && (frame = grabber.grab()) != null) {
                    //���ո�֡���뵽out��
                    record.record(frame);
                    //����չʾ��frame
                    canvas.showImage(frame);
                    //����ѭ��ѹ��
                    Thread.sleep(30);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            } finally {
                //�ر���Դ
                try {
                    canvas.dispose();
                    record.close();
                    grabber.close();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return this;
    }

    /**
     * ת��װ
     *
     * @throws IOException
     */
    public RtmpTpRtmp2 go() throws IOException {
        long err_index = 0;//�ɼ����������µĴ������
        //�������û�вɼ���֡����Ϊ��Ƶ�ɼ���������������������1�μ��жϳ���
        for (int no_frame_index = 0; no_frame_index < 5 || err_index > 1; ) {
            avcodec.AVPacket pkt = null;
            try {
                //û�н��������Ƶ֡
                pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
                    //�հ���¼��������
                    no_frame_index++;
                    continue;
                }
                //����Ҫ����ֱ�Ӱ�����Ƶ֡�Ƴ�ȥ
                err_index += (record.recordPacket(pkt) ? 0 : 1);//���ʧ��err_index����1
                System.out.println(err_index);
                av_free_packet(pkt);
            } catch (IOException e) {//����ʧ��
                System.out.println("++++++++++++++++++++++++++++++++++++++++++");
                err_index++;
            } catch (Exception e) {
                System.out.println("-------------------------------------------");
                err_index++;
            }
        }
        return this;
    }
}
