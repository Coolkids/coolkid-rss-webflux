package com.coolkid.coolkidrss.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class KryoUtil {
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 设置循环引用
        kryo.setReferences(true);
        // 设置序列化时对象是否需要设置对象类型
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static <T extends Serializable> byte[] serialize(T object) {
        if (object == null) {
            return EMPTY_BYTE_ARRAY;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Output output = new Output(baos)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            // 对象的 Class 信息一起序列化
            kryo.writeClassAndObject(output, object);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SerializationException("Could not write byte[]: " + e.getMessage(), e);
        } finally {
            KRYO_THREAD_LOCAL.remove();
        }
    }

    public static <T extends Serializable> T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); Input input = new Input(bais)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            // 通过存储在字节数组中的 Class 信息来确定反序列的类型
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        } finally {
            KRYO_THREAD_LOCAL.remove();
        }
    }
}

