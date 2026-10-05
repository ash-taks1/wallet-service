package net.alishahidi.vehiclecrossing.walletservice.mapper;

import net.alishahidi.vehiclecrossing.walletservice.dto.response.RegisterResponseDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.UserEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "walletId", source = "wallet.id")
    @Mapping(target = "currency", source = "wallet.currency")
    RegisterResponseDto toRegisterResponse(UserEntity user, WalletEntity wallet);
}

