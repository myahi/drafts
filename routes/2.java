List<Path> files = s.filter(Files::isRegularFile).filter(p -> matcher == null || matcher.matches(p.getFileName())).sorted(cmp).collect(Collectors.toList());
