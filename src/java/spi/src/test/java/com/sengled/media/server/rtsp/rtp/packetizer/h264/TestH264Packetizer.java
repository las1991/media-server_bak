package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import com.sengled.media.FramePacket;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.rtsp.rtp.RTP;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import junit.framework.TestCase;

public class TestH264Packetizer extends TestCase {
    String rtp1 = "80600248cbe534877ef09c6b5c819a09a5f08421e439e02382f02b01600406023832029e2bffcfe47efeb6031def9425169c740f322dd8d839fe953823c5cbe37c8556796a5004efb96a4f6281010a59e60ea896bb732495966401e20b3b2770ab5560cafdda8947da65666beaa6552cd2db011030c7bbdcedd7e73a62f692daa2a5f87e50216ffb62b82da65d9d47da4614df6ce53388f2ef4fcce3b35742eb6c78308dcf2ee16c2245a86f64198f02a6c8d8ef19ac7d0dc79b34b82d8133d872301be248b2c10df308a82eaa7250b25edaf9441ec83bd4b11e6f0c8d2fdddb8bad23005e7315cf4fdb319ef0e5b8811126564ea9ed7db135bf48ff6d177cc22d4f092bd180205cc8edd388c79434affa7e6453665d7e0a7a8c9a424a0adc71a8561a6e2f6162646d63ae1a2128f96ba11c0bf54646017f44ec4ca328974397a6d578da1b7b1822fd5c47cc7646cba8dba2062fb0d23945298b6c7b5715c650070413874ba687ace473e5f5a70dcce9b5d17fcac5f9ceba4968342c86d2404643236aa04a6ae5d78ba4343e3782a8a9c9f85c2f71dc7be664fdd576be61c0254ac77ae6651cf42e40b3a836ff33ddf5ad52eae45166dc1363f89358d6704a1b27ab9471a79abc16fa5e6f944ffd5bcece939346743302ca3fdae807e59048a81037697f5f63d4fc05d0edb56ab48856defa4aaa457ab27ac6768529816a3e2ce56639798997bbb2b2b255c78b11512e42ee595f4e6755ef66e7cab1dda758f7d858e884d8b32cd03c4ada6b03c559945eace3ccecb35d0a44f47af68ea4ff6ef96d2a87f9f8448ab62f713fa0f8ec8a798c1480d040145d21d0c9d779b5d5ce9c3e5df003ffb548461f29c357c624d4a109df1c72aaf0ca9e9d7f01486d2de5ee0a1abc1f650b36f8a680e7ba7613bccfcc55ce3a9dc5536471f4b281bc1d3dd15afde14008797cc02e94ea6f7617da0be06be08361ddc3ea3ab342dcdec4241ab96c3b0e118cb39b409eb50441766b43d225825740703fcf3d9290b934f75d618fe2369afa7bffd4f677d97fde14f4a2e79a0edd6c418e4fe6e0a7c9a396a3c6465f15a1839532a398b7047cc4fc207910a198b83497f1b0e703697856c2ebf33d9e2c2352ae6e4163e07874a3a1d171cc6d750e52578066cac9989fc27fe4fdc447770eaf3c5d7798447416e7a1c7d365cb7eea0cf81e2cb6164c8c32eb117bcf775eabbb9bb1d85416276cc96522250f34161705d2c0f8d8ed912cec950ede96401711ff5a57a0d2c00dd880db93dd114856382e64de53a681220c67eb1b105822e9b8ea1a7f712dbf33111833999269b7eb30674a3eab9a286e1e90e403ad21dcfb2da0c2bb5d8c080fb3c5c31044d726a810bdf1d61a2c64be7cfd174e2849feab6d1c1ab6009774fe02c57931c446c013288a80e8f3be5309ac25a67f57cfdb3b9688dfdd5926ef566cc9811541baf0fb1a17fb0a5dbbaa0ec7fd16e039196053edf2d32e3233b732b0cfaa1c426fed6d65ffcf93d91b913b1422a6c57b2f3d6dcf0595a48a2c2fd86d1fbdb9a620a9d24569c26b9338ddc5f2ed2907a059f558c98e6b792af8a2ef124c931153946c01e7046ef14fbfede9486a71b22952ebc3f4a9e9bc4dbc794ad835308a6da403204d6a19670a8763b67066d7c9c6542689983d1ac43c8d577d14181d17c7041911120780da492aecd40713fb39f09a3eba455388a2dfb0b08e0e4b96af5156429a551ab203924156a38f9b9fd11f14e45acaf8235ac646042d9aafec51dd8a34918212a9cc57b21f4e5eedba8932f04976dce4845aff9a1111dce99c9f668afacc089e98bc90a80aee38e2e50536e157a6cccbc597c8f5c86b40693e3e479b1ddce9186c308997a19481d123a8138f445c91795221d9533acbfd4f1950ffaf5d650e1e496890f043a545111dbebf67e10b237b80b970a64c417c5011f06cc119369f42b188a10ea98b7526f6465b8b3cfea4059e179af8d35e9419f16860d01c1b5040dceb4e9d0cfd58127d57180eb9740589b181876bb44f78b9120fbeb9d82b620d9bd4f771cb3963e47a99";
    String rtp2 = "80e00249cbe534877ef09c6b5c41071effa8f814f27ccc6c9ccb34bd3b59fdf726118b06a021f7af1ddcf29b3652e2f889870d6b3ce2";

    public void testPacket() throws DecoderException, IOException {
        H264DePacketizer de = new H264DePacketizer(0, new AVCDecoderConfigurationRecord(), Rational.$90_000);

        
        final ByteBuf[] rtps = new ByteBuf[] {
                Unpooled.wrappedBuffer(Hex.decodeHex(rtp1.toCharArray())).retain(8),
                Unpooled.wrappedBuffer(Hex.decodeHex(rtp2.toCharArray())).retain(8),
        };

        List<Object> out = new ArrayList<>();
        de.dePacket(RTP.decode(rtps[0].duplicate().retain()), out);
        de.dePacket(RTP.decode(rtps[1].duplicate().retain()), out);
        assertEquals(1, out.size());
        
        final FramePacket frame = (FramePacket)out.remove(0);

        H264Packetizer pa = new H264Packetizer(0, de);
        List<Object> rtpOut = new ArrayList<>();
        pa.packet(frame.duplicate().retain(), rtpOut);
        
        ByteBuf[] decodec = new ByteBuf[] {
                RTP.encode((RtpPacketI)rtpOut.get(1)),
                RTP.encode((RtpPacketI)rtpOut.get(2)),
        };

        de.dePacket(RTP.decode(decodec[0].duplicate().retain()), out);
        de.dePacket(RTP.decode(decodec[1].duplicate().retain()), out);
        final FramePacket newFrame = (FramePacket)out.remove(0);
        

        System.out.println(ByteBufUtil.prettyHexDump(frame.content()));
        System.out.println(ByteBufUtil.prettyHexDump(newFrame.content()));
        assertEquals(frame.content(),  newFrame.content());
        
        
        de.close();
        pa.close();
    }
}

