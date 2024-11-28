package drawable;

Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp), // 텍스트와 입력 필드 사이의 간격 설정
        verticalAlignment = Alignment.CenterVertically // 세로 방향 중앙 정렬
    ) {
        // '카테고리' 텍스트
        Text(text = "카테고리", modifier = Modifier.align(Alignment.CenterVertically))