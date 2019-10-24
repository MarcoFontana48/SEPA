/* The class represents the credentials of a generic identity
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class Credentials implements Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7444283028651497389L;
	private String user;
	private String password;

	public Credentials(String user, String password) {
		if (user == null || password == null)
			throw new IllegalArgumentException("User or password are null");
		this.user = user;
		this.password = password;
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public String getBasicAuthorizationHeader() throws SEPASecurityException {
		String plainString = user + ":" + password;
		try {
			return "Basic " + new String(Base64.getEncoder().encode(plainString.getBytes("UTF-8")), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SEPASecurityException(e);
		}
	}
	
	public byte[] serialize() throws SEPASecurityException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(this);
			out.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new SEPASecurityException(e);
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {

			}
		}	
	}
	
	public static Credentials deserialize(byte[] stream) throws SEPASecurityException {
		ByteArrayInputStream bis = new ByteArrayInputStream(stream);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		 return (Credentials) in.readObject(); 
		} catch (IOException | ClassNotFoundException e) {
			throw new SEPASecurityException(e);
		} finally {
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		  }
		}	
	}	
}
