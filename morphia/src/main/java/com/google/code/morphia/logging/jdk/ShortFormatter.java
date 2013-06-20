package com.google.code.morphia.logging.jdk;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;


public class ShortFormatter extends Formatter {
  private static final MessageFormat FORMAT = new MessageFormat("[{1}|{2}|{3,date,h:mm:ss}]{0} :{4}");
  private static final Logr          LOG    = MorphiaLoggerFactory.get(ShortFormatter.class);

  @Override
  public String format(final LogRecord record) {
    final StringBuilder sb = new StringBuilder();
    String source = record.getSourceClassName() == null ? record.getLoggerName() : record.getSourceClassName();
    source = source.substring(source.length() - 15) + "." + (record.getSourceMethodName() == null ? "" : record.getSourceMethodName());

    final Object[] arguments = new Object[6];
    arguments[0] = source;
    arguments[1] = record.getLevel();
    arguments[2] = Thread.currentThread().getName();
    arguments[3] = new Date(record.getMillis());
    arguments[4] = record.getMessage();
    sb.append(FORMAT.format(arguments));

    if (record.getThrown() != null) {
      try {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        sb.append(sw.toString());
      } catch (Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
    sb.append("\n");
    return sb.toString();
  }

}
