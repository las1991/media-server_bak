package com.sengled.media.server.rtsp.rtp;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import junit.framework.TestCase;

public class TestRTPCodec extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRTPCodec.class);
    

    String hex2 = "8060252e3a287ae3c160af6b5c015903b02cbdbf198d61c199e946fe0a019c2bf6e638336851062cf264111bfef4010838be6bc5c5fa38f4adff8b27e4650f904d2c1120005564a1a5ea3b7caa7a4b64b695de5e2dd1498dc0030d11f3ab459d44f9f38672582f0d652b9f3694434e50659c30b1bacc38c9d7ff7c514de145b6697dcc4dd151b279ddbc1ef877d5bcf7c72ef1e5a3ca5ef7eb0ab7ea3286ccc20fd58e075f3183b4f5229fb869f4a2121eaf1a7074689b2691f25bbe274df33e4d6edaf38b3659dc365a001018dc1d13a48b61db23643e91aeea8bcd2b8159b244b5cddb3068c274cf47d1ecdfb20bf49c6609470e77f3fcdf049d28a3d0ee6656c8ebb0514ad296415de3a5062289626755c61de9387eb38f674b4fc65bb7ca52a45317a9d3e05939377fdb88f16edf14bc947afaaaaff45caf4cda4d777a5e16b1bdae7ae23d29cf541d4c2de219fdff66da028305bfd2b1e3d97b238bfa9c7a82ab3bd9dd1748d6d4a81c3e52f80555ebc297a905f112d0f9ae94ccb186604cd32237ce508a6090a6c36240935ea51057b6069d63035012a6af0b7bbdf05e183b37aeb16bbef2535856209cbfce82d3ba456e741279bf5ca08241854bdd046c40a9b4384afc3d6c08822e975bc3d810282238d8716254b8a99b864cdc14f7c6b07a392457f6d50b1f227e070f37523e41edbd4d6026e6b40b310504d2bfef027e18488cc3e274235d9230d953e566937e168bd222ad8f100194b9da00be06330b8cadf1525e1bf1cc7b0d39082e1f91ee9f0c2913ef44fa53114881ab88be3cfbf8205ba2f2bea9c495445c251e684e4d9819667ca7e64e2148bc261fd60b4c8a3b06c755cc877da55dedc66bcb556233c8b1723e6321514373e16b208d286e73301d9a1522274f1f6636dfd48f20a167e186cccdac303077151ae3a9e9d51bcd5597a7eebbef4837b4587f45a6ce7d2fd3cb4b255633d258bd3ab86f45c698b0d0da8988b69504ef5db725d1ee9d3eb91c98212ae6d3dc05b7a3fa86c8f806fa6c922fd6ede86c7bb0c2b216f88d498ad7b3cc1a499581505d6731eda40584d121e566e437f167e5308420aff0a2bbd5c96953a69e606edfd43ef849e8bc0f160237d32b0f6ae273a4c6bb9b238ead9b217b1b44db62e6e9c4e5fd4cfd3193b10879280328b03e13ed7e01b4391683d807ee70afa6b1393f560d495765985b9f85bdbfe7e212a953eb6352d636e177aa84f395c1cfba8a804817276d00fe897a2e5868e33163e5ba2a630a33de965bd8376c3196c08498ea322b19a9e850c53cf13e96feb48d5b833a95a6dacb62f26f77392868e9c1f1dae92ddaa94be40fd0e920fd2dcfcc507b0b90ca33a4fbde6fcf9a1f78fcc8d76d7a0ed283b531a40218a2d33889d608d7d433e64ac11ad58defa6c2e4a869a25ca20cf35f31a274a7ecfa66d603bb402f2834ac0cfef0f7095b9b2e7a8f4abf9dd48afaa0d7e078f93f25a6a06dcfec9666177efb013c3023d548af6a9d5fd8a6640256823d9851837e49e802fa14efb55ff5db1d24559a7b86a8229bb96afcff220288179ca2b776964c3f47325a37883ce547637981f648fed0a325fcbb273e2ef6dc77e1f6897610b038cfe33e3655ac71c96a516789002503a2c697192c01c5861d52832df097ad69ab6602b2a9dd83e4fa46db38fc62fe96a8de4d621e5fbf6da541810486189f9e8e50146544473dceec1956bbd9b41fe071e5562f3bec027e49534b216398fa749b9387eb47096d693cf6da8dd4a704edf445360378427facb33135fd28be8c092176fc6772de76e28197147fbfc859c6438a5ab0bdd28ff4e45b099f1aff1aa078466c98a3c6d2ee481fe7367bccefc0c46b4a2a697696a04bdfcf794971e1e593e9c4dd0343af47010734b14d684eda0e2498c98345f216042ad749d9d8dcfd536b99ef4703f0b066be8ab04a5fdc4507ff502862577e7b968c9cfe3a378711bb17a0b5b82d95a4e9690904283932e079ca0d70d6ff695f182f41c0d04a557080f8d023166c880d644abe927d20d76a27f66137dbcd3c8da010951b";
    
    public void testDecode() throws DecoderException {
        byte[] data = Hex.decodeHex(hex2.toCharArray());
        
        RtpPacketI rtp = RTP.decode(Unpooled.wrappedBuffer(data));
        System.out.println(rtp);
        
        ByteBuf newRTP = RTP.encode(rtp);
        assertEquals(hex2, ByteBufUtil.hexDump(newRTP));
        
        newRTP = RTP.encode(rtp.copyFast());
        assertEquals(hex2, ByteBufUtil.hexDump(newRTP));
        
        newRTP = RTP.encode(rtp.copyFast());
        assertEquals(hex2, ByteBufUtil.hexDump(newRTP));
        
        //long ssrc = rtp.getSyncSource();
        //rtp.setSyncSource(ssrc);   
        //newRTP = DefaultRtpPacket.encode(rtp.copyFast());
        //assertEquals(hex2, ByteBufUtil.hexDump(newRTP));
    }
}
