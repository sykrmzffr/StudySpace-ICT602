package com.example.studyspace.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.studyspace.data.model.Booking;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BookingDao_Impl implements BookingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Booking> __insertionAdapterOfBooking;

  private final EntityDeletionOrUpdateAdapter<Booking> __deletionAdapterOfBooking;

  private final SharedSQLiteStatement __preparedStmtOfClearUserBookings;

  public BookingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBooking = new EntityInsertionAdapter<Booking>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bookings_table` (`bookingId`,`userId`,`roomId`,`roomName`,`roomLocation`,`date`,`startTime`,`endTime`,`status`,`checkedIn`,`isPendingSync`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Booking entity) {
        if (entity.getBookingId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getBookingId());
        }
        if (entity.getUserId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getUserId());
        }
        if (entity.getRoomId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getRoomId());
        }
        if (entity.getRoomName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getRoomName());
        }
        if (entity.getRoomLocation() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getRoomLocation());
        }
        if (entity.getDate() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDate());
        }
        if (entity.getStartTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getStartTime());
        }
        if (entity.getEndTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEndTime());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getStatus());
        }
        final int _tmp = entity.isCheckedIn() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.isPendingSync() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
      }
    };
    this.__deletionAdapterOfBooking = new EntityDeletionOrUpdateAdapter<Booking>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `bookings_table` WHERE `bookingId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Booking entity) {
        if (entity.getBookingId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getBookingId());
        }
      }
    };
    this.__preparedStmtOfClearUserBookings = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookings_table WHERE userId = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertBooking(final Booking booking) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfBooking.insert(booking);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAll(final List<Booking> bookings) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfBooking.insert(bookings);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteBooking(final Booking booking) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfBooking.handle(booking);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearUserBookings(final String userId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearUserBookings.acquire();
    int _argIndex = 1;
    if (userId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, userId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearUserBookings.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Booking>> getUserBookings(final String userId) {
    final String _sql = "SELECT * FROM bookings_table WHERE userId = ? ORDER BY date DESC, startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (userId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, userId);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"bookings_table"}, false, new Callable<List<Booking>>() {
      @Override
      @Nullable
      public List<Booking> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfBookingId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookingId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfRoomId = CursorUtil.getColumnIndexOrThrow(_cursor, "roomId");
          final int _cursorIndexOfRoomName = CursorUtil.getColumnIndexOrThrow(_cursor, "roomName");
          final int _cursorIndexOfRoomLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "roomLocation");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCheckedIn = CursorUtil.getColumnIndexOrThrow(_cursor, "checkedIn");
          final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
          final List<Booking> _result = new ArrayList<Booking>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Booking _item;
            _item = new Booking();
            final String _tmpBookingId;
            if (_cursor.isNull(_cursorIndexOfBookingId)) {
              _tmpBookingId = null;
            } else {
              _tmpBookingId = _cursor.getString(_cursorIndexOfBookingId);
            }
            _item.setBookingId(_tmpBookingId);
            final String _tmpUserId;
            if (_cursor.isNull(_cursorIndexOfUserId)) {
              _tmpUserId = null;
            } else {
              _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            }
            _item.setUserId(_tmpUserId);
            final String _tmpRoomId;
            if (_cursor.isNull(_cursorIndexOfRoomId)) {
              _tmpRoomId = null;
            } else {
              _tmpRoomId = _cursor.getString(_cursorIndexOfRoomId);
            }
            _item.setRoomId(_tmpRoomId);
            final String _tmpRoomName;
            if (_cursor.isNull(_cursorIndexOfRoomName)) {
              _tmpRoomName = null;
            } else {
              _tmpRoomName = _cursor.getString(_cursorIndexOfRoomName);
            }
            _item.setRoomName(_tmpRoomName);
            final String _tmpRoomLocation;
            if (_cursor.isNull(_cursorIndexOfRoomLocation)) {
              _tmpRoomLocation = null;
            } else {
              _tmpRoomLocation = _cursor.getString(_cursorIndexOfRoomLocation);
            }
            _item.setRoomLocation(_tmpRoomLocation);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item.setDate(_tmpDate);
            final String _tmpStartTime;
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null;
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
            }
            _item.setStartTime(_tmpStartTime);
            final String _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
            }
            _item.setEndTime(_tmpEndTime);
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            _item.setStatus(_tmpStatus);
            final boolean _tmpCheckedIn;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCheckedIn);
            _tmpCheckedIn = _tmp != 0;
            _item.setCheckedIn(_tmpCheckedIn);
            final boolean _tmpIsPendingSync;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPendingSync);
            _tmpIsPendingSync = _tmp_1 != 0;
            _item.setPendingSync(_tmpIsPendingSync);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<Booking> getPendingBookings() {
    final String _sql = "SELECT * FROM bookings_table WHERE isPendingSync = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfBookingId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookingId");
      final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
      final int _cursorIndexOfRoomId = CursorUtil.getColumnIndexOrThrow(_cursor, "roomId");
      final int _cursorIndexOfRoomName = CursorUtil.getColumnIndexOrThrow(_cursor, "roomName");
      final int _cursorIndexOfRoomLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "roomLocation");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
      final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfCheckedIn = CursorUtil.getColumnIndexOrThrow(_cursor, "checkedIn");
      final int _cursorIndexOfIsPendingSync = CursorUtil.getColumnIndexOrThrow(_cursor, "isPendingSync");
      final List<Booking> _result = new ArrayList<Booking>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Booking _item;
        _item = new Booking();
        final String _tmpBookingId;
        if (_cursor.isNull(_cursorIndexOfBookingId)) {
          _tmpBookingId = null;
        } else {
          _tmpBookingId = _cursor.getString(_cursorIndexOfBookingId);
        }
        _item.setBookingId(_tmpBookingId);
        final String _tmpUserId;
        if (_cursor.isNull(_cursorIndexOfUserId)) {
          _tmpUserId = null;
        } else {
          _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
        }
        _item.setUserId(_tmpUserId);
        final String _tmpRoomId;
        if (_cursor.isNull(_cursorIndexOfRoomId)) {
          _tmpRoomId = null;
        } else {
          _tmpRoomId = _cursor.getString(_cursorIndexOfRoomId);
        }
        _item.setRoomId(_tmpRoomId);
        final String _tmpRoomName;
        if (_cursor.isNull(_cursorIndexOfRoomName)) {
          _tmpRoomName = null;
        } else {
          _tmpRoomName = _cursor.getString(_cursorIndexOfRoomName);
        }
        _item.setRoomName(_tmpRoomName);
        final String _tmpRoomLocation;
        if (_cursor.isNull(_cursorIndexOfRoomLocation)) {
          _tmpRoomLocation = null;
        } else {
          _tmpRoomLocation = _cursor.getString(_cursorIndexOfRoomLocation);
        }
        _item.setRoomLocation(_tmpRoomLocation);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item.setDate(_tmpDate);
        final String _tmpStartTime;
        if (_cursor.isNull(_cursorIndexOfStartTime)) {
          _tmpStartTime = null;
        } else {
          _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
        }
        _item.setStartTime(_tmpStartTime);
        final String _tmpEndTime;
        if (_cursor.isNull(_cursorIndexOfEndTime)) {
          _tmpEndTime = null;
        } else {
          _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
        }
        _item.setEndTime(_tmpEndTime);
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        _item.setStatus(_tmpStatus);
        final boolean _tmpCheckedIn;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfCheckedIn);
        _tmpCheckedIn = _tmp != 0;
        _item.setCheckedIn(_tmpCheckedIn);
        final boolean _tmpIsPendingSync;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsPendingSync);
        _tmpIsPendingSync = _tmp_1 != 0;
        _item.setPendingSync(_tmpIsPendingSync);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
