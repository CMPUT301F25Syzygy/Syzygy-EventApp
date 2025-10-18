# MainActivity
## responsibility
- Allow switching between different fragments
- Provide a navbar to choose fragments
- By default show a navbar button to open `ProfileFragment`
- By default show a navbar button to open `FindEventsFragment`
- By default show a navbar button to open `JoinedEventsFragment`
- By default show a navbar button to open `OrganizerFragment`
- By default show a navbar button to open `AdministatorFragment`
- Interupt current fragment with an `InviteFragment` when an invite comes from the server, or when the app is opened to a pending invite
- Allow fragments to open subfragments
- Allow subfragments to exit to their parent fragment
- Allow fragments to customize the navbar with different items suited to their needs
## collaborators
- ProfileFragment
- FindEventsFragment
- JoinedEventsFragment
- OrganizerFragment
- AdministatorFragment
- InviteFragment