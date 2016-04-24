/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that allows to add basic sort and filter functionality to the
 * search.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class QueryHelper {

  @SuppressWarnings("serial")
  public static class InvalidQueryException extends Exception {

    public InvalidQueryException(String msg) {
      super(msg);
    }
  }

  /**
   * Unknown participantId used by {@link ASC_CREATOR_COMPARATOR} in case wave
   * creator cannot be found.
   */
  static final ParticipantId UNKNOWN_CREATOR = ParticipantId.ofUnsafe("unknown@example.com");

  /** Sorts search result in ascending order by LMT (Last Modified Time). */
  static final Comparator<WaveViewData> ASC_LMT_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      long lmt0 = computeLmt(arg0);
      long lmt1 = computeLmt(arg1);
      return Long.signum(lmt0 - lmt1);
    }

    private long computeLmt(WaveViewData wave) {
      long lmt = -1;
      for (ObservableWaveletData wavelet : wave.getWavelets()) {
        // Skip non conversational wavelets.
        if (!IdUtil.isConversationalId(wavelet.getWaveletId())) {
          continue;
        }
        lmt = lmt < wavelet.getLastModifiedTime() ? wavelet.getLastModifiedTime() : lmt;
      }
      return lmt;
    }
  };

  /** Sorts search result in descending order by LMT (Last Modified Time). */
  public static final Comparator<WaveViewData> DESC_LMT_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          return -ASC_LMT_COMPARATOR.compare(arg0, arg1);
        }
      };

  /** Sorts search result in ascending order by creation time. */
  public static final Comparator<WaveViewData> ASC_CREATED_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          long time0 = computeCreatedTime(arg0);
          long time1 = computeCreatedTime(arg1);
          return Long.signum(time0 - time1);
        }

    private long computeCreatedTime(WaveViewData wave) {
      long creationTime = -1;
      for (ObservableWaveletData wavelet : wave.getWavelets()) {
        creationTime =
            creationTime < wavelet.getCreationTime() ? wavelet.getCreationTime() : creationTime;
      }
      return creationTime;
    }
  };

  /** Sorts search result in descending order by creation time. */
  public static final Comparator<WaveViewData> DESC_CREATED_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          return -ASC_CREATED_COMPARATOR.compare(arg0, arg1);
        }
      };

  /** Sorts search result in ascending order by creator */
  public static final Comparator<WaveViewData> ASC_CREATOR_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          ParticipantId creator0 = computeCreator(arg0);
          ParticipantId creator1 = computeCreator(arg1);
          return creator0.compareTo(creator1);
        }

    private ParticipantId computeCreator(WaveViewData wave) {
      for (ObservableWaveletData wavelet : wave.getWavelets()) {
        if (IdUtil.isConversationRootWaveletId(wavelet.getWaveletId())) {
          return wavelet.getCreator();
        }
      }
      // If not found creator - compare with UNKNOWN_CREATOR;
      return UNKNOWN_CREATOR;
    }
  };

  /** Sorts search result in descending order by creator */
  public static final Comparator<WaveViewData> DESC_CREATOR_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          return -ASC_CREATOR_COMPARATOR.compare(arg0, arg1);
        }
      };

  /** Sorts search result by WaveId. */
  public static final Comparator<WaveViewData> ID_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      return arg0.getWaveId().compareTo(arg1.getWaveId());
    }
  };

  /**
   * Orders using {@link ASCENDING_DATE_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> ASC_LMT_ORDERING = Ordering
      .from(QueryHelper.ASC_LMT_COMPARATOR);

  /**
   * Orders using {@link DESCENDING_DATE_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> DESC_LMT_ORDERING = Ordering
      .from(QueryHelper.DESC_LMT_COMPARATOR);

  /**
   * Orders using {@link ASC_CREATED_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> ASC_CREATED_ORDERING = Ordering
      .from(QueryHelper.ASC_CREATED_COMPARATOR);

  /**
   * Orders using {@link DESC_CREATED_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> DESC_CREATED_ORDERING = Ordering
      .from(QueryHelper.DESC_CREATED_COMPARATOR);

  /**
   * Orders using {@link ASC_CREATOR_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> ASC_CREATOR_ORDERING = Ordering
      .from(QueryHelper.ASC_CREATOR_COMPARATOR);

  /**
   * Orders using {@link DESC_CREATOR_COMPARATOR}.
   */
  public static final Ordering<WaveViewData> DESC_CREATOR_ORDERING = Ordering
      .from(QueryHelper.DESC_CREATOR_COMPARATOR);

  /** Default ordering is by LMT descending. */
  public static final Ordering<WaveViewData> DEFAULT_ORDERING = DESC_LMT_ORDERING;

  /** Registered order by parameter types and corresponding orderings. */
  public enum OrderByValueType {
    DATEASC("dateasc", ASC_LMT_ORDERING),
    DATEDESC("datedesc", DESC_LMT_ORDERING),
    CREATEDASC("createdasc", ASC_CREATED_ORDERING),
    CREATEDDESC("createddesc", DESC_CREATED_ORDERING),
    CREATORASC("creatorasc", ASC_CREATOR_ORDERING),
    CREATORDESC("creatordesc", DESC_CREATOR_ORDERING);

    final String token;
    final Ordering<WaveViewData> ordering;

    OrderByValueType(String value, Ordering<WaveViewData> ordering) {
      this.token = value;
      this.ordering = ordering;
    }

    public String getToken() {
      return token;
    }

    public Ordering<WaveViewData> getOrdering() {
      return ordering;
    }

    private static final Map<String, OrderByValueType> reverseLookupMap =
      new HashMap<String, OrderByValueType>();

    static {
      for (OrderByValueType type : OrderByValueType.values()) {
        reverseLookupMap.put(type.getToken(), type);
      }
    }

    public static OrderByValueType fromToken(String token) {
      OrderByValueType orderByValue = reverseLookupMap.get(token);
      if (orderByValue == null) {
        throw new IllegalArgumentException("Illegal 'orderby' value: " + token);
      }
      return reverseLookupMap.get(token);
    }
  }

  /**
   * Builds a list of participants to serve as the filter for the query.
   *
   * @param queryParams the query params.
   * @param queryType the filter for the query , i.e. 'with'.
   * @param localDomain the local domain of the logged in user.
   * @return the participants list for the filter.
   * @throws InvalidParticipantAddress if participant id passed to the query is invalid.
   */
  public static List<ParticipantId> buildValidatedParticipantIds(
      Map<TokenQueryType, Set<String>> queryParams,
      TokenQueryType queryType, String localDomain) throws InvalidParticipantAddress {
    Set<String> tokenSet = queryParams.get(queryType);
    List<ParticipantId> participants = null;
    if (tokenSet != null) {
      participants = Lists.newArrayListWithCapacity(tokenSet.size());
      for (String token : tokenSet) {
        if (!token.isEmpty() && token.indexOf("@") == -1) {
          // If no domain was specified, assume that the participant is from the local domain.
          token = token + "@" + localDomain;
        } else if (token.equals("@")) {
          // "@" is a shortcut for the shared domain participant.
          token = "@" + localDomain;
        }
        ParticipantId otherUser = ParticipantId.of(token);
        participants.add(otherUser);
      }
    } else {
      participants = Collections.emptyList();
    }
    return participants;
  }

  /**
   * Computes ordering for the search results. If none are specified - then
   * returns the default ordering. The resulting ordering is always compounded
   * with ordering by wave id for stability.
   */
  public static Ordering<WaveViewData> computeSorter(
      Map<TokenQueryType, Set<String>> queryParams) {
    Ordering<WaveViewData> ordering = null;
    Set<String> orderBySet = queryParams.get(TokenQueryType.ORDERBY);
    if (orderBySet != null) {
      for (String orderBy : orderBySet) {
        QueryHelper.OrderByValueType orderingType =
            QueryHelper.OrderByValueType.fromToken(orderBy);
        if (ordering == null) {
          // Primary ordering.
          ordering = orderingType.getOrdering();
        } else {
          // All other ordering are compounded to the primary one.
          ordering = ordering.compound(orderingType.getOrdering());
        }
      }
    } else {
      ordering = QueryHelper.DEFAULT_ORDERING;
    }
    // For stability order also by wave id.
    ordering = ordering.compound(QueryHelper.ID_COMPARATOR);
    return ordering;
  }

  /**
   * Parses the search query.
   *
   * @param query the query.
   * @return the result map with query tokens. Never returns null.
   * @throws InvalidQueryException if the query contains invalid params.
   */
  public static Map<TokenQueryType, Set<String>> parseQuery(String query)
      throws InvalidQueryException {
    Preconditions.checkArgument(query != null);
    query = query.trim();
    // If query is empty - return.
    if (query.isEmpty()) {
      return Collections.emptyMap();
    }
    String[] tokens = query.split("\\s+");
    Map<TokenQueryType, Set<String>> tokensMap = Maps.newEnumMap(TokenQueryType.class);
    for (String token : tokens) {
      String[] pair = token.split(":");
      if (pair.length != 2 || !TokenQueryType.hasToken(pair[0])) {
        String msg = "Invalid query param: " + token;
        throw new InvalidQueryException(msg);
      }
      String tokenValue = pair[1];
      TokenQueryType tokenType = TokenQueryType.fromToken(pair[0]);
      // Verify the orderby param.
      if (tokenType.equals(TokenQueryType.ORDERBY)) {
        try {
          OrderByValueType.fromToken(tokenValue);
        } catch (IllegalArgumentException e) {
          String msg = "Invalid orderby query value: " + tokenValue;
          throw new InvalidQueryException(msg);
        }
      }
      Set<String> valuesPerToken = tokensMap.get(tokenType);
      if (valuesPerToken == null) {
        valuesPerToken = Sets.newLinkedHashSet();
        tokensMap.put(tokenType, valuesPerToken);
      }
      valuesPerToken.add(tokenValue);
    }
    return tokensMap;
  }

  /** Private constructor to prevent instantiation. */
  private QueryHelper() {}
}
