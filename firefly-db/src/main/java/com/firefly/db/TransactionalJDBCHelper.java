package com.firefly.db;

import java.sql.Connection;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TransactionalJDBCHelper {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	private static final ThreadLocal<Transaction> transaction = new ThreadLocal<>();
	private final JDBCHelper jdbcHelper;

	public TransactionalJDBCHelper(JDBCHelper jdbcHelper) {
		this.jdbcHelper = jdbcHelper;
	}

	public void beginTransaction() {
		getTransaction().beginTransaction();
	}

	public void commit() {
		getTransaction().commit();
	}

	public void rollback() {
		getTransaction().rollback();
	}

	public void endTransaction() {
		getTransaction().endTransaction();
	}

	private Transaction getTransaction() {
		Transaction t = transaction.get();
		if (t == null) {
			t = new Transaction();
			transaction.set(t);
		}
		return t;
	}

	enum Status {
		INIT, START, COMMIT, ROLLBACK, END
	}

	class Transaction {
		private Connection connection;
		private Status status = Status.INIT;
		private int count = 0;

		void beginTransaction() {
			if (status == Status.INIT) {
				connection = jdbcHelper.getConnection();
				jdbcHelper.setAutoCommit(connection, false);
				status = Status.START;
			}
			count++;
			log.debug("begin transaction {}", count);
		}

		Connection getConnection() {
			if (status != Status.START) {
				throw new IllegalStateException("The transaction status exception, " + status);
			}

			return connection;
		}

		void rollback() {
			status = Status.ROLLBACK;
		}

		void commit() {
			status = Status.COMMIT;
		}

		void endTransaction() {
			count--;
			if (count == 0) {
				switch (status) {
				case START:
				case COMMIT:
					jdbcHelper.commit(connection);
					break;
				case ROLLBACK:
					jdbcHelper.rollback(connection);
					break;
				default:
					break;
				}

				jdbcHelper.close(connection);
				transaction.set(null);
				status = Status.END;
			}
			log.debug("end transaction {}", count);
		}

	}

}
