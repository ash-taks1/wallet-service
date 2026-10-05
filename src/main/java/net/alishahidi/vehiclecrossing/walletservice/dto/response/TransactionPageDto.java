package net.alishahidi.vehiclecrossing.walletservice.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** One page of history; pass nextCursor as ?cursor= to get the next (older) page. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionPageDto {
    List<TransactionDto> items;
    UUID nextCursor;
}
