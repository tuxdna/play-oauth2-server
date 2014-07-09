package models

import play.api.libs.Codecs

object Helpers {

  /**
   * @param password A string of password characters
   * @return SHA1 of @password represented as Hex characters
   */
  def encodePassword(password: String) = {
    Codecs.sha1(password)
  }

}