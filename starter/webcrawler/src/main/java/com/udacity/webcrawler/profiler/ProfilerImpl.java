package com.udacity.webcrawler.profiler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
    //Profiler.wrap() should throw an IllegalArgumentException if the wrapped interface does not contain a @Profiled method.
    boolean isProfiledMethod = false;
    for (Method method : klass.getMethods()) {
      if (method.getAnnotation(Profiled.class) != null) {
        isProfiledMethod = true;
        break;
      }
    }
    if (isProfiledMethod == false) {
      throw new IllegalArgumentException("The class " + klass.getName() + " does not contain a @Profiled method.");
    }
    ProfilingMethodInterceptor profilingMethodInterceptor = new ProfilingMethodInterceptor(delegate, clock, state);
    Object proxy = Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class[]{klass},profilingMethodInterceptor
            );
    return (T)proxy;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
  try (Writer writer= new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND))){
    writeData(writer);
  } catch (IOException ex) {
    ex.printStackTrace();
  }
  }


  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
