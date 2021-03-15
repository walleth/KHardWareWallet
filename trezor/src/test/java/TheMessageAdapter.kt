import com.google.common.truth.Truth.assertThat
import data.featurePacket
import io.trezor.deviceprotocol.EthereumAddress
import io.trezor.deviceprotocol.EthereumSignMessage
import io.trezor.deviceprotocol.Features
import io.trezor.deviceprotocol.MessageType
import okio.ByteString
import org.junit.Assert.fail
import org.junit.Test
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.model.HexString
import org.walleth.khartwarewallet.trezor.messages.getMessageId
import org.walleth.khartwarewallet.trezor.messages.parseMessageWithType
import org.walleth.khartwarewallet.trezor.messages.withFrameAsBuffer

class TheMessageAdapter {
    @Test
    fun canDecodePackage() {

        val decoded = featurePacket.parseMessageWithType(MessageType.MessageType_Features)

        if (decoded !is Features) {
            fail("packet should be instance of Features")
        } else {
            assertThat(decoded.vendor).isEqualTo("trezor.io")
            assertThat(decoded.major_version).isEqualTo(2)
            assertThat(decoded.minor_version).isEqualTo(1)
            assertThat(decoded.patch_version).isEqualTo(0)
            assertThat(decoded.initialized).isEqualTo(true)
        }
    }

    @Test
    fun foo() {
        val payload = HexString("0x122a307842346332304539663039463965364262636133386139313861303441454532423337376531313333").hexToByteArray()

        val res = EthereumAddress.ADAPTER.decode(payload)

    }
    @Test
    fun canDecodeMessageId() {
        val message = EthereumSignMessage.Builder().message(ByteString.EMPTY).build()
        assertThat(getMessageId(message)).isEqualTo(108)
    }

    @Test
    fun canEncodeMessageFrame() {
        val tested = Features.Builder().build().withFrameAsBuffer()
        assertThat(String(tested.readByteArray())).startsWith("##")
    }
}