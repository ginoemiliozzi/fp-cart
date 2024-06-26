package algebras

import algebras.LiveCrypto.{DecryptCipher, EncryptCipher}

import java.util.Base64
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import cats.effect.Sync
import cats.syntax.all._
import config.model.PasswordSalt
import eu.timepit.refined.auto._
import io.estatico.newtype.macros.newtype
import model.user.{EncryptedPassword, Password}

trait Crypto {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}

object LiveCrypto {

  @newtype case class EncryptCipher(value: Cipher)
  @newtype case class DecryptCipher(value: Cipher)
  def make[F[_]: Sync](secret: PasswordSalt): F[Crypto] =
    Sync[F]
      .delay {
        val fixedIV = "FixedInitVector!" // (16 characters)
        val ivBytes = fixedIV.getBytes("UTF-8")
        val iv = new IvParameterSpec(ivBytes)
        val salt = secret.value.value.getBytes("UTF-8")
        val keySpec = new PBEKeySpec("password".toCharArray, salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes = factory.generateSecret(keySpec).getEncoded
        val sKeySpec = new SecretKeySpec(bytes, "AES")
        val eCipher = EncryptCipher(Cipher.getInstance("AES/CBC/PKCS5Padding"))
        eCipher.value.init(Cipher.ENCRYPT_MODE, sKeySpec, iv)
        val dCipher = DecryptCipher(Cipher.getInstance("AES/CBC/PKCS5Padding"))
        dCipher.value.init(Cipher.DECRYPT_MODE, sKeySpec, iv)
        (eCipher, dCipher)
      }
      .map { case (ec, dc) =>
        new LiveCrypto(ec, dc)
      }
}

final class LiveCrypto private (
    eCipher: EncryptCipher,
    dCipher: DecryptCipher
) extends Crypto {

  def encrypt(password: Password): EncryptedPassword = {
    val base64 = Base64.getEncoder()
    val bytes = password.value.getBytes("UTF-8")
    val result =
      new String(base64.encode(eCipher.value.doFinal(bytes)), "UTF-8")
    EncryptedPassword(result)
  }

  def decrypt(password: EncryptedPassword): Password = {
    val base64 = Base64.getDecoder()
    val bytes = base64.decode(password.value.getBytes("UTF-8"))
    val result = new String(dCipher.value.doFinal(bytes), "UTF-8")
    Password(result)
  }

}
