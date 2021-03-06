/*
 * regain - A file search engine providing plenty of formats
 * Copyright (C) 2004  Til Schneider
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Contact: Til Schneider, info@murfman.de
 */
package net.sf.regain.crawler.document;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.Token;

/**
 * A token stream reading tokens from an iterator.
 * (Currently not used.)
 *
 * @author Til Schneider, www.murfman.de
 */
public class IteratorTokenStream /*extends TokenStream*/ {

  /** An iterator providing Strings. */
  private Iterator mIter;


  /**
   * Creates a new instance of this class.
   *
   * @param iter An iterator providing Strings.
   */
  public IteratorTokenStream(Iterator iter) {
    mIter = iter;
  }


  // overridden
  public Token next() throws IOException {
    if (mIter.hasNext()) {
      String text = (String) mIter.next();
      return new Token(text, 0, text.length());
    } else {
      return null;
    }
  }

}
