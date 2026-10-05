package net.alishahidi.vehiclecrossing.walletservice.mapper;

import java.util.List;

import net.alishahidi.vehiclecrossing.walletservice.dto.response.WalletDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface WalletMapper {

    @Mapping(target = "walletId", source = "id")
    WalletDto toDto(WalletEntity wallet);

    List<WalletDto> toDtos(List<WalletEntity> wallets);
}
