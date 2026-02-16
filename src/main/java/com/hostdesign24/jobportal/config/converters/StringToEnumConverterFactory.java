package com.hostdesign24.jobportal.config.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

@Component
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

  @Override
  public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToEnumConverter<>(targetType);
  }

  private record StringToEnumConverter<T extends Enum>(Class<T> enumType) implements
      Converter<String, T> {

    @Override
      @SuppressWarnings("unchecked")
      public T convert(String source) {
        if (source.isEmpty()) {
          return null;
        }
        return (T) Enum.valueOf(enumType, source.trim().toUpperCase());
      }
    }
}
