package com.digitalsanctuary.spring.user.api.provider;

import com.digitalsanctuary.spring.user.api.data.ApiTestData;
import com.digitalsanctuary.spring.user.api.data.DataStatus;
import com.digitalsanctuary.spring.user.api.provider.holder.ApiTestArgumentsHolder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class ApiTestUpdateUserArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                new ApiTestArgumentsHolder(
                        ApiTestData.getUserDto(),
                        DataStatus.LOGGED,
                        ApiTestData.userUpdateSuccess()
                ),
                new ApiTestArgumentsHolder(
                        ApiTestData.getUserDto(),
                        DataStatus.NOT_LOGGED,
                        ApiTestData.userNotLogged()
                )
        ).map(Arguments::of);
    }
}
